<?php

namespace Tests\Feature\Api;

use App\Models\Especialidad;
use App\Models\TurnoPersonal;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PublicoPersonalDemoTest extends TestCase
{
    use RefreshDatabase;

    public function test_personal_demo_lista_medicos_y_apoyo_con_estado_de_turno(): void
    {
        $esp = Especialidad::firstOrCreate(['nombre' => 'Pediatría']);

        $medicoEnTurno = User::factory()->create([
            'rol' => 'MEDICO',
            'id_especialidad' => $esp->getKey(),
            'estado' => 'ACTIVO',
            'usuario' => 'medico.pediatria.demo',
        ]);
        $medicoFuera = User::factory()->create([
            'rol' => 'MEDICO',
            'id_especialidad' => $esp->getKey(),
            'estado' => 'ACTIVO',
            'usuario' => 'medico.pediatria.fuera',
        ]);
        $enfermera = User::factory()->create([
            'rol' => 'RECEPCIONISTA',
            'cargo' => 'Enfermera',
            'estado' => 'ACTIVO',
            'usuario' => 'enfermeria.demo',
        ]);
        User::factory()->create([
            'rol' => 'ADMINISTRADOR',
            'estado' => 'ACTIVO',
            'usuario' => 'admin.oculto',
        ]);

        TurnoPersonal::create([
            'id_usuario' => $medicoEnTurno->getKey(),
            'fecha' => today()->toDateString(),
            'hora_inicio' => '00:00:00',
            'hora_fin' => '23:59:59',
            'area' => 'Consulta Externa',
        ]);

        $res = $this->getJson('/api/v1/publico/personal-demo');
        $res->assertOk()->assertJsonPath('success', true);

        $items = collect($res->json('data'));
        $this->assertFalse($items->contains('usuario', 'admin.oculto'));

        $enTurno = $items->firstWhere('usuario', 'medico.pediatria.demo');
        $this->assertNotNull($enTurno);
        $this->assertTrue($enTurno['en_turno']);
        $this->assertSame('MEDICO', $enTurno['categoria']);
        $this->assertSame('Pediatría', $enTurno['subgrupo']);

        $fuera = $items->firstWhere('usuario', 'medico.pediatria.fuera');
        $this->assertNotNull($fuera);
        $this->assertFalse($fuera['en_turno']);

        $enf = $items->firstWhere('usuario', 'enfermeria.demo');
        $this->assertNotNull($enf);
        $this->assertSame('APOYO', $enf['categoria']);
        $this->assertSame('Enfermería', $enf['subgrupo']);
    }

    public function test_personal_demo_se_bloquea_en_produccion_sin_debug(): void
    {
        config(['app.debug' => false]);
        app()->detectEnvironment(fn () => 'production');

        $this->getJson('/api/v1/publico/personal-demo')->assertNotFound();
    }
}
