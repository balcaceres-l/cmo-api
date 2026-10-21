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

class AgendaTest extends TestCase
{
    use RefreshDatabase;

    private User $medico;

    private Paciente $paciente;

    private string $fecha;

    protected function setUp(): void
    {
        parent::setUp();
        $this->fecha = today()->addDay()->toDateString();
        $this->medico = User::factory()->create(['rol' => 'MEDICO']);
        TurnoPersonal::create(['id_usuario' => $this->medico->id, 'fecha' => $this->fecha, 'hora_inicio' => '08:00', 'hora_fin' => '10:00', 'area' => 'Consulta general']);
        $this->paciente = Paciente::create(['numero_expediente' => 'EXP-2026-000901', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $this->paciente->id_paciente]);
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
    }

    private function agendar(array $cambios = [])
    {
        return $this->postJson('/api/v1/citas', $cambios + ['id_paciente' => $this->paciente->id_paciente, 'id_medico' => $this->medico->id, 'fecha' => $this->fecha, 'hora_inicio' => '08:00', 'motivo' => 'Control']);
    }

    public function test_disponibilidad_respeta_turno_y_citas_ocupadas(): void
    {
        $this->agendar()->assertCreated();
        Cita::create(['id_paciente' => $this->paciente->id_paciente, 'id_medico' => $this->medico->id, 'fecha' => $this->fecha, 'hora_inicio' => '09:00', 'hora_fin' => '09:30', 'motivo' => 'x', 'estado' => 'CANCELADA', 'id_creado_por' => $this->medico->id]);

        $bloques = $this->getJson("/api/v1/agenda/disponibilidad?id_medico={$this->medico->id}&fecha={$this->fecha}")
            ->assertOk()->json('data');

        // Turno de 2 h en bloques de 30 min = 4; la cita de las 08:00 ocupa uno y la cancelada no.
        $this->assertSame(['08:30', '09:00', '09:30'], array_column($bloques, 'hora_inicio'));
    }

    public function test_reglas_al_agendar(): void
    {
        $this->agendar(['hora_inicio' => '11:00'])->assertStatus(422);
        $this->agendar()->assertCreated()->assertJsonPath('data.hora_fin', '08:30');
        $this->agendar()->assertStatus(409);
        $this->agendar(['id_medico' => User::factory()->create(['rol' => 'RECEPCIONISTA'])->id])
            ->assertStatus(422)->assertJsonValidationErrors('id_medico');
    }

    public function test_no_se_agenda_en_el_pasado_ni_cruzando_la_medianoche(): void
    {
        TurnoPersonal::create(['id_usuario' => $this->medico->id, 'fecha' => today()->subDay(), 'hora_inicio' => '08:00', 'hora_fin' => '10:00', 'area' => 'Consulta general']);
        TurnoPersonal::create(['id_usuario' => $this->medico->id, 'fecha' => $this->fecha, 'hora_inicio' => '20:00', 'hora_fin' => '23:59', 'area' => 'Emergencia']);

        $this->agendar(['fecha' => today()->subDay()->toDateString()])->assertStatus(422)->assertJsonValidationErrors('fecha');
        // 23:45 + 30 min terminaría a las 00:15: dos citas idénticas ya no se detectarían como solapadas.
        $this->agendar(['hora_inicio' => '23:45'])->assertStatus(422)->assertJsonValidationErrors('hora_inicio');
        $this->agendar(['hora_inicio' => '23:00'])->assertCreated();
        $this->agendar(['hora_inicio' => '23:00'])->assertStatus(409);
    }
}
