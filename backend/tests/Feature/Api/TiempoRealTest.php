<?php

namespace Tests\Feature\Api;

use App\Events\AmbulanciaActualizada;
use App\Events\OcupacionActualizada;
use App\Exceptions\ConflictoException;
use App\Models\Ambulancia;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\ServicioAmbulancia;
use App\Models\User;
use App\Services\AmbulanciaService;
use App\Services\AsignacionCamaService;
use Database\Seeders\AmbulanciaSeeder;
use Database\Seeders\HabitacionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Event;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class TiempoRealTest extends TestCase
{
    use RefreshDatabase;

    public function test_despacho_se_guarda_aunque_reverb_no_responda(): void
    {
        $this->seed(AmbulanciaSeeder::class);
        // Reverb configurado pero sin nadie escuchando en ese puerto.
        config([
            'broadcasting.default' => 'reverb',
            'broadcasting.connections.reverb.options.host' => '127.0.0.1',
            'broadcasting.connections.reverb.options.port' => 6999,
            'broadcasting.connections.reverb.options.scheme' => 'http',
        ]);
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));

        $this->postJson('/api/v1/ambulancia/servicios', ['paciente_nombre' => 'Persona', 'direccion' => 'Centro', 'descripcion' => 'Caída', 'prioridad' => 'ALTA'])
            ->assertCreated();
        $this->assertSame('EN_SERVICIO', Ambulancia::value('estado'));
        $this->assertSame(1, ServicioAmbulancia::count());
    }

    public function test_no_se_emite_evento_si_la_operacion_falla(): void
    {
        $this->seed(AmbulanciaSeeder::class);
        Ambulancia::first()->update(['estado' => 'EN_MANTENIMIENTO']);
        Event::fake([AmbulanciaActualizada::class]);

        try {
            app(AmbulanciaService::class)->despachar(['id_solicitante' => User::factory()->create()->id, 'paciente_nombre' => 'P', 'direccion' => 'D', 'descripcion' => 'E', 'prioridad' => 'ALTA']);
        } catch (ConflictoException) {
        }

        Event::assertNotDispatched(AmbulanciaActualizada::class);
    }

    public function test_ocupacion_se_emite_tras_el_commit(): void
    {
        $this->seed(HabitacionSeeder::class);
        Event::fake([OcupacionActualizada::class]);
        $m = User::factory()->create(['rol' => 'MEDICO']);
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-000701', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);

        app(AsignacionCamaService::class)->asignar($p, 'VIP', null, $m, 'Ingreso', $m);

        Event::assertDispatchedTimes(OcupacionActualizada::class, 1);
    }
}
