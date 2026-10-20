<?php

namespace Tests\Feature\Api;

use App\Models\Cita;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\TurnoPersonal;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class TratanteTest extends TestCase
{
    use RefreshDatabase;

    private function paciente(): Paciente
    {
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-000501', 'nombre_completo' => 'Paciente Ajeno', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente, 'antecedentes_personales' => 'Dato reservado']);

        return $p;
    }

    private function medicoConTurno(): User
    {
        $m = User::factory()->create(['rol' => 'MEDICO']);
        TurnoPersonal::create(['id_usuario' => $m->id, 'fecha' => today()->addDay(), 'hora_inicio' => '08:00', 'hora_fin' => '12:00', 'area' => 'Consulta general']);

        return $m;
    }

    public function test_medico_no_puede_agendar_a_un_paciente_que_no_atiende(): void
    {
        $p = $this->paciente();
        Sanctum::actingAs($this->medicoConTurno());

        $this->postJson('/api/v1/citas', ['id_paciente' => $p->id_paciente, 'fecha' => today()->addDay()->toDateString(), 'hora_inicio' => '08:00', 'motivo' => 'Control'])
            ->assertForbidden();
        $this->getJson("/api/v1/pacientes/{$p->id_paciente}/expediente")->assertForbidden();
        $this->assertSame(0, Cita::count());
    }

    public function test_medico_agenda_seguimiento_de_su_paciente(): void
    {
        $p = $this->paciente();
        $m = $this->medicoConTurno();
        Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $m->id, 'fecha' => today(), 'hora_inicio' => '07:00', 'hora_fin' => '07:30', 'motivo' => 'Primera', 'estado' => 'ATENDIDA', 'id_creado_por' => $m->id]);
        Sanctum::actingAs($m);

        $this->postJson('/api/v1/citas', ['id_paciente' => $p->id_paciente, 'fecha' => today()->addDay()->toDateString(), 'hora_inicio' => '08:00', 'motivo' => 'Seguimiento'])
            ->assertCreated();
        $this->getJson("/api/v1/pacientes/{$p->id_paciente}/expediente")->assertOk()->assertSee('Dato reservado');
    }

    public function test_cita_cancelada_o_solicitada_no_da_acceso_clinico(): void
    {
        $p = $this->paciente();
        $m = $this->medicoConTurno();
        foreach (['CANCELADA', 'SOLICITADA', 'NO_ASISTIO'] as $estado) {
            Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $m->id, 'fecha' => today(), 'hora_inicio' => '07:00', 'hora_fin' => '07:30', 'motivo' => 'x', 'estado' => $estado, 'id_creado_por' => $m->id]);
        }
        Sanctum::actingAs($m);

        $this->getJson("/api/v1/pacientes/{$p->id_paciente}/expediente")->assertForbidden();
        $this->getJson('/api/v1/pacientes')->assertOk()->assertJsonPath('data.total', 0);
    }
}
