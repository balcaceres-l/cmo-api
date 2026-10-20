<?php

namespace Tests\Feature\Api;

use App\Models\Cita;
use App\Models\ExamenCatalogo;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ConsultaTest extends TestCase
{
    use RefreshDatabase;

    public function test_flujo_completo_y_consulta_cerrada_es_de_solo_lectura(): void
    {
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-001001', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $cita = Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $medico->id, 'fecha' => today(), 'hora_inicio' => '08:00', 'hora_fin' => '08:30', 'motivo' => 'Dolor de cabeza', 'estado' => 'EN_ESPERA', 'id_creado_por' => $medico->id]);
        $examen = ExamenCatalogo::create(['nombre' => 'Hemograma completo', 'activo' => true]);
        $receta = ['medicamento' => 'Acetaminofén', 'dosis' => '500 mg', 'frecuencia' => 'Cada 8 h', 'duracion' => '3 días'];

        Sanctum::actingAs(User::factory()->create(['rol' => 'MEDICO']));
        $this->postJson("/api/v1/citas/{$cita->id_cita}/consulta")->assertForbidden();

        Sanctum::actingAs($medico);
        $id = $this->postJson("/api/v1/citas/{$cita->id_cita}/consulta")->assertCreated()->json('data.id_consulta');
        $this->putJson("/api/v1/consultas/{$id}", ['diagnostico' => 'Migraña'])->assertOk()->assertJsonPath('data.diagnostico', 'Migraña');
        $this->postJson("/api/v1/consultas/{$id}/recetas", $receta)->assertCreated();
        $this->postJson("/api/v1/consultas/{$id}/ordenes-laboratorio", ['id_examen' => $examen->id_examen])->assertCreated()->assertJsonPath('data.estado', 'PENDIENTE');
        $this->patchJson("/api/v1/consultas/{$id}/finalizar")->assertOk();
        $this->assertSame('ATENDIDA', $cita->refresh()->estado);

        $this->putJson("/api/v1/consultas/{$id}", ['diagnostico' => 'Otro'])->assertStatus(409);
        $this->postJson("/api/v1/consultas/{$id}/recetas", $receta)->assertStatus(409);
        $this->postJson("/api/v1/consultas/{$id}/ordenes-laboratorio", ['id_examen' => $examen->id_examen])->assertStatus(409);
        $this->getJson("/api/v1/consultas/{$id}")->assertOk()->assertJsonCount(1, 'data.recetas')->assertJsonCount(1, 'data.ordenes_laboratorio');
    }
}
