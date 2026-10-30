<?php

namespace Tests\Feature\Api;

use App\Models\Cita;
use App\Models\ExamenCatalogo;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Carbon\CarbonInterface;
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

    public function test_abandonar_una_consulta_abierta_devuelve_la_cita_a_su_estado_anterior(): void
    {
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-001002', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $nueva = fn (string $estado, ?string $llegada = null) => Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $medico->id, 'fecha' => today(), 'hora_inicio' => '08:00', 'hora_fin' => '08:30', 'motivo' => 'Control', 'estado' => $estado, 'hora_llegada' => $llegada, 'id_creado_por' => $medico->id]);
        $receta = ['medicamento' => 'Acetaminofén', 'dosis' => '500 mg', 'frecuencia' => 'Cada 8 h', 'duracion' => '3 días'];

        // El paciente no había llegado (AGENDADA): vuelve a AGENDADA y la consulta desaparece junto con sus recetas.
        $cita = $nueva('AGENDADA');
        Sanctum::actingAs(User::factory()->create(['rol' => 'MEDICO']));
        Sanctum::actingAs($medico);
        $id = $this->postJson("/api/v1/citas/{$cita->id_cita}/consulta")->assertCreated()->json('data.id_consulta');
        $this->postJson("/api/v1/consultas/{$id}/recetas", $receta)->assertCreated();

        Sanctum::actingAs(User::factory()->create(['rol' => 'MEDICO']));
        $this->patchJson("/api/v1/consultas/{$id}/abandonar")->assertForbidden();
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $this->patchJson("/api/v1/consultas/{$id}/abandonar")->assertForbidden();

        Sanctum::actingAs($medico);
        $this->patchJson("/api/v1/consultas/{$id}/abandonar")->assertOk()->assertJsonPath('data.estado', 'AGENDADA')->assertJsonPath('message', 'Consulta abandonada.');
        $this->assertSame('AGENDADA', $cita->refresh()->estado);
        $this->assertDatabaseMissing('consulta', ['id_consulta' => $id]);
        $this->assertDatabaseMissing('receta', ['id_consulta' => $id]);
        $this->getJson("/api/v1/consultas/{$id}")->assertNotFound();
        $this->patchJson("/api/v1/consultas/{$id}/abandonar")->assertNotFound();

        // Se puede volver a iniciar la consulta de esa cita.
        $this->postJson("/api/v1/citas/{$cita->id_cita}/consulta")->assertCreated();

        // El paciente ya había llegado (EN_ESPERA): vuelve a EN_ESPERA.
        $espera = $nueva('EN_ESPERA', now());
        $espera->update(['hora_inicio' => '09:00', 'hora_fin' => '09:30']);
        $id2 = $this->postJson("/api/v1/citas/{$espera->id_cita}/consulta")->assertCreated()->json('data.id_consulta');
        $this->patchJson("/api/v1/consultas/{$id2}/abandonar")->assertOk()->assertJsonPath('data.estado', 'EN_ESPERA');
        $this->assertNotNull($espera->refresh()->hora_llegada);
    }

    public function test_se_abandona_con_ordenes_sin_iniciar_pero_no_con_una_que_el_laboratorio_ya_trabaja_ni_cerrada(): void
    {
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $recepcion = User::factory()->create(['rol' => 'RECEPCIONISTA']);
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-001003', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $cita = Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $medico->id, 'fecha' => today(), 'hora_inicio' => '08:00', 'hora_fin' => '08:30', 'motivo' => 'Control', 'estado' => 'AGENDADA', 'id_creado_por' => $medico->id]);
        $examen = ExamenCatalogo::create(['nombre' => 'Hemograma completo', 'activo' => true]);

        // Órdenes PENDIENTE y CANCELADA: el laboratorio no hizo nada, así que se descartan con la consulta.
        Sanctum::actingAs($medico);
        $id = $this->postJson("/api/v1/citas/{$cita->id_cita}/consulta")->assertCreated()->json('data.id_consulta');
        $orden = $this->postJson("/api/v1/consultas/{$id}/ordenes-laboratorio", ['id_examen' => $examen->id_examen])->assertCreated()->json('data.id_orden');
        $this->postJson("/api/v1/consultas/{$id}/ordenes-laboratorio", ['id_examen' => $examen->id_examen])->assertCreated();
        $this->patchJson("/api/v1/ordenes-laboratorio/{$orden}/cancelar")->assertOk();
        $this->patchJson("/api/v1/consultas/{$id}/abandonar")->assertOk()->assertJsonPath('data.estado', 'AGENDADA');
        $this->assertDatabaseMissing('consulta', ['id_consulta' => $id]);
        $this->assertDatabaseCount('orden_laboratorio', 0);
        $this->assertSame('AGENDADA', $cita->refresh()->estado);

        // Si el laboratorio ya inició una orden, no se abandona: hay que finalizar.
        $id = $this->postJson("/api/v1/citas/{$cita->id_cita}/consulta")->assertCreated()->json('data.id_consulta');
        $trabajada = $this->postJson("/api/v1/consultas/{$id}/ordenes-laboratorio", ['id_examen' => $examen->id_examen])->assertCreated()->json('data.id_orden');
        Sanctum::actingAs($recepcion);
        $this->patchJson("/api/v1/ordenes-laboratorio/{$trabajada}/iniciar")->assertOk();
        Sanctum::actingAs($medico);
        $this->patchJson("/api/v1/consultas/{$id}/abandonar")->assertStatus(409);
        $this->assertSame('EN_ATENCION', $cita->refresh()->estado);
        $this->assertDatabaseHas('consulta', ['id_consulta' => $id]);

        // Cerrada es de solo lectura: tampoco se abandona.
        $this->patchJson("/api/v1/consultas/{$id}/finalizar")->assertOk();
        $this->patchJson("/api/v1/consultas/{$id}/abandonar")->assertStatus(409);
        $this->assertSame('ATENDIDA', $cita->refresh()->estado);
    }

    public function test_la_consulta_solo_se_inicia_el_dia_de_la_cita(): void
    {
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-001004', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $en = fn (CarbonInterface $dia, string $hora) => Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $medico->id, 'fecha' => $dia, 'hora_inicio' => $hora, 'hora_fin' => '23:59', 'motivo' => 'Control', 'estado' => 'AGENDADA', 'id_creado_por' => $medico->id]);
        $manana = $en(today()->addDay(), '08:00');
        $ayer = $en(today()->subDay(), '08:00');
        $hoy = $en(today(), '08:00');

        Sanctum::actingAs($medico);
        $this->postJson("/api/v1/citas/{$manana->id_cita}/consulta")->assertStatus(409)->assertJsonPath('message', 'La consulta solo se inicia el día de la cita.');
        $this->postJson("/api/v1/citas/{$ayer->id_cita}/consulta")->assertStatus(409);
        $this->assertSame('AGENDADA', $manana->refresh()->estado);
        $this->assertDatabaseCount('consulta', 0);

        $this->postJson("/api/v1/citas/{$hoy->id_cita}/consulta")->assertCreated();
        $this->assertSame('EN_ATENCION', $hoy->refresh()->estado);
    }
}
