<?php

namespace Tests\Feature\Api;

use App\Models\Cita;
use App\Models\Consulta;
use App\Models\ExamenCatalogo;
use App\Models\HistorialClinico;
use App\Models\OrdenLaboratorio;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class LaboratorioTest extends TestCase
{
    use RefreshDatabase;

    public function test_el_laboratorio_solo_informa_estados(): void
    {
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $recepcion = User::factory()->create(['rol' => 'RECEPCIONISTA']);
        $cuenta = User::factory()->create(['rol' => 'PACIENTE']);
        $p = Paciente::create(['id_usuario' => $cuenta->id, 'numero_expediente' => 'EXP-2026-000099', 'nombre_completo' => 'Paciente', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $cita = Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $medico->id, 'fecha' => today(), 'hora_inicio' => '08:00', 'hora_fin' => '08:30', 'motivo' => 'Control', 'estado' => 'EN_ATENCION', 'id_creado_por' => $medico->id]);
        $consulta = Consulta::create(['id_cita' => $cita->id_cita, 'id_medico' => $medico->id, 'fecha_hora_inicio' => now()]);
        $ex = ExamenCatalogo::create(['nombre' => 'Hemograma', 'activo' => true]);
        $o = OrdenLaboratorio::create(['id_consulta' => $consulta->id_consulta, 'id_paciente' => $p->id_paciente, 'id_examen' => $ex->id_examen, 'id_medico' => $medico->id, 'fecha_solicitud' => now(), 'estado' => 'PENDIENTE']);
        Sanctum::actingAs($cuenta);
        $this->getJson('/api/v1/mi/laboratorio')->assertOk()->assertJsonPath('data.data.0.estado', 'PENDIENTE');

        Sanctum::actingAs($recepcion);
        $this->patchJson("/api/v1/ordenes-laboratorio/{$o->id_orden}/completar")->assertStatus(409);
        $this->patchJson("/api/v1/ordenes-laboratorio/{$o->id_orden}/iniciar")->assertOk()->assertJsonPath('data.estado', 'EN_PROCESO');
        $this->patchJson("/api/v1/ordenes-laboratorio/{$o->id_orden}/iniciar")->assertStatus(409);
        // Completar no pide ni guarda datos del examen: solo cambia el estado.
        $this->patchJson("/api/v1/ordenes-laboratorio/{$o->id_orden}/completar")->assertOk()
            ->assertJsonPath('data.estado', 'COMPLETADO')
            ->assertJsonMissingPath('data.resultado');

        foreach ([$cuenta, $medico] as $u) {
            Sanctum::actingAs($u);
            $ruta = $u->is($cuenta) ? '/api/v1/mi/laboratorio' : '/api/v1/ordenes-laboratorio';
            $this->getJson($ruta)->assertOk()
                ->assertJsonPath('data.data.0.estado', 'COMPLETADO')
                ->assertJsonMissingPath('data.data.0.resultado');
        }
    }
}
