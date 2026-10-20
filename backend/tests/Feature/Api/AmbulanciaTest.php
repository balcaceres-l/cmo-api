<?php

namespace Tests\Feature\Api;

use App\Events\AmbulanciaActualizada;
use App\Exceptions\ConflictoException;
use App\Models\Ambulancia;
use App\Models\User;
use App\Services\AmbulanciaService;
use Database\Seeders\AmbulanciaSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Event;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class AmbulanciaTest extends TestCase
{
    use RefreshDatabase;

    public function test_despacho_finalizacion_y_evento(): void
    {
        $this->seed(AmbulanciaSeeder::class);
        Event::fake();
        $u = User::factory()->create();
        $s = app(AmbulanciaService::class)->despachar(['id_solicitante' => $u->id, 'paciente_nombre' => 'Persona', 'direccion' => 'Centro', 'descripcion' => 'Emergencia', 'prioridad' => 'ALTA']);
        $this->assertSame('EN_SERVICIO', Ambulancia::first()->estado);
        app(AmbulanciaService::class)->finalizar($s);
        $this->assertSame('DISPONIBLE', Ambulancia::first()->estado);
        Event::assertDispatched(AmbulanciaActualizada::class);
    }

    public function test_medico_ve_estado_sin_datos_de_la_persona_atendida(): void
    {
        $this->seed(AmbulanciaSeeder::class);
        $recepcion = User::factory()->create(['rol' => 'RECEPCIONISTA']);
        app(AmbulanciaService::class)->despachar(['id_solicitante' => $recepcion->id, 'paciente_nombre' => 'Juana Perez', 'direccion' => 'Colonia Centro', 'descripcion' => 'Caída', 'prioridad' => 'ALTA']);

        Sanctum::actingAs(User::factory()->create(['rol' => 'MEDICO']));
        $this->getJson('/api/v1/ambulancia')->assertOk()
            ->assertJsonPath('data.estado', 'EN_SERVICIO')
            ->assertJsonPath('data.servicio_activo.prioridad', 'ALTA')
            ->assertDontSee('Juana Perez')
            ->assertDontSee('Colonia Centro');

        Sanctum::actingAs($recepcion);
        $this->getJson('/api/v1/ambulancia')->assertOk()->assertSee('Juana Perez');
    }

    public function test_no_despacha_en_mantenimiento(): void
    {
        $this->seed(AmbulanciaSeeder::class);
        Ambulancia::first()->update(['estado' => 'EN_MANTENIMIENTO']);
        $this->expectException(ConflictoException::class);
        app(AmbulanciaService::class)->despachar([]);
    }
}
