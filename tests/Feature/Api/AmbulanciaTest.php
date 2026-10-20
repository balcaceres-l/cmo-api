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

    public function test_no_despacha_en_mantenimiento(): void
    {
        $this->seed(AmbulanciaSeeder::class);
        Ambulancia::first()->update(['estado' => 'EN_MANTENIMIENTO']);
        $this->expectException(ConflictoException::class);
        app(AmbulanciaService::class)->despachar([]);
    }
}
