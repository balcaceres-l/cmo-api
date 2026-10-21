<?php

namespace Tests\Feature\Api;

use App\Events\AmbulanciaActualizada;
use App\Events\ReporteAmbulanciaRecibido;
use App\Models\ServicioAmbulancia;
use App\Models\User;
use App\Services\AmbulanciaService;
use Database\Seeders\AmbulanciaSeeder;
use Illuminate\Broadcasting\PrivateChannel;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Event;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ReporteAmbulanciaTest extends TestCase
{
    use RefreshDatabase;

    private User $paramedico;

    private ServicioAmbulancia $servicio;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(AmbulanciaSeeder::class);
        $this->paramedico = User::factory()->create(['rol' => 'RECEPCIONISTA', 'cargo' => 'Paramédico']);
        $this->servicio = app(AmbulanciaService::class)->despachar(['id_solicitante' => $this->paramedico->id, 'paciente_nombre' => 'Persona Herida', 'direccion' => 'Carretera km 98', 'descripcion' => 'Accidente', 'prioridad' => 'CRITICA']);
    }

    private function reporte(array $extra = [])
    {
        return $this->postJson("/api/v1/ambulancia/servicios/{$this->servicio->id_servicio}/reportes", $extra + ['estado_paciente' => 'CRITICO', 'observaciones' => 'Consciente, sangrado en pierna izquierda', 'minutos_llegada' => 12]);
    }

    public function test_la_ambulancia_avisa_antes_de_llegar_por_un_canal_privado(): void
    {
        Event::fake([ReporteAmbulanciaRecibido::class, AmbulanciaActualizada::class]);
        Sanctum::actingAs($this->paramedico);

        $this->reporte()->assertCreated()
            ->assertJsonPath('data.estado_paciente', 'CRITICO')
            ->assertJsonPath('data.id_autor', $this->paramedico->id);

        Event::assertDispatched(ReporteAmbulanciaRecibido::class, function ($e) {
            $datos = $e->broadcastWith();

            return $e->broadcastOn() == [new PrivateChannel('personal')]
                && $datos['estado_paciente'] === 'CRITICO'
                && $datos['llegada_estimada'] !== null
                && ! in_array('Persona Herida', $datos, true);
        });
    }

    public function test_emergencia_ve_el_ultimo_reporte_sin_datos_personales(): void
    {
        Sanctum::actingAs($this->paramedico);
        $this->reporte(['estado_paciente' => 'DELICADO', 'minutos_llegada' => 20])->assertCreated();
        $this->reporte(['estado_paciente' => 'CRITICO', 'minutos_llegada' => 8])->assertCreated();

        Sanctum::actingAs(User::factory()->create(['rol' => 'MEDICO', 'cargo' => 'Emergencia']));
        $this->getJson('/api/v1/ambulancia')->assertOk()
            ->assertJsonPath('data.servicio_activo.ultimo_reporte.estado_paciente', 'CRITICO')
            ->assertDontSee('Persona Herida')
            ->assertDontSee('Carretera km 98');
        $this->getJson("/api/v1/ambulancia/servicios/{$this->servicio->id_servicio}/reportes")->assertOk()
            ->assertJsonCount(2, 'data')
            ->assertJsonPath('data.0.estado_paciente', 'DELICADO');
    }

    public function test_solo_con_servicio_en_curso_y_solo_personal(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'PACIENTE']));
        $this->reporte()->assertForbidden();

        Sanctum::actingAs($this->paramedico);
        $this->reporte(['estado_paciente' => 'GRAVE'])->assertStatus(422);
        app(AmbulanciaService::class)->finalizar($this->servicio);
        $this->reporte()->assertStatus(409);
    }

    public function test_el_canal_personal_excluye_a_pacientes(): void
    {
        // Lee la regla registrada en routes/channels.php para el canal "personal".
        $canales = (fn () => $this->channels)->call(app('Illuminate\Broadcasting\BroadcastManager')->connection());
        $autoriza = $canales['personal'];

        $this->assertTrue($autoriza($this->paramedico));
        $this->assertTrue($autoriza(User::factory()->create(['rol' => 'MEDICO'])));
        $this->assertFalse($autoriza(User::factory()->create(['rol' => 'PACIENTE'])));
        $this->assertFalse($autoriza(User::factory()->create(['rol' => 'MEDICO', 'estado' => 'INACTIVO'])));
    }
}
