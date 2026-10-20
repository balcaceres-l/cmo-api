<?php

namespace Tests\Feature\Api;

use App\Models\Cita;
use App\Models\Consulta;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\TurnoPersonal;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class EstadosCitaTest extends TestCase
{
    use RefreshDatabase;

    private Paciente $paciente;

    private User $medico;

    protected function setUp(): void
    {
        parent::setUp();
        $this->paciente = Paciente::create(['numero_expediente' => 'EXP-2026-000801', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $this->paciente->id_paciente]);
        $this->medico = User::factory()->create(['rol' => 'MEDICO']);
        TurnoPersonal::create(['id_usuario' => $this->medico->id, 'fecha' => today()->addDay(), 'hora_inicio' => '08:00', 'hora_fin' => '12:00', 'area' => 'Consulta general']);
    }

    private function cita(string $estado): Cita
    {
        return Cita::create(['id_paciente' => $this->paciente->id_paciente, 'id_medico' => $this->medico->id, 'fecha' => today(), 'hora_inicio' => '08:00', 'hora_fin' => '08:30', 'motivo' => 'Control', 'estado' => $estado, 'id_creado_por' => $this->medico->id]);
    }

    public function test_recepcion_no_altera_citas_cerradas(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $atendida = $this->cita('ATENDIDA');
        $cancelada = $this->cita('CANCELADA');
        $agendada = $this->cita('AGENDADA');
        $manana = ['id_medico' => $this->medico->id, 'fecha' => today()->addDay()->toDateString(), 'hora_inicio' => '09:00'];

        $this->patchJson("/api/v1/citas/{$atendida->id_cita}/cancelar", ['motivo_cancelacion' => 'x'])->assertStatus(409);
        $this->patchJson("/api/v1/citas/{$atendida->id_cita}/reprogramar", $manana)->assertStatus(409);
        $this->patchJson("/api/v1/citas/{$cancelada->id_cita}/llegada")->assertStatus(409);
        $this->patchJson("/api/v1/citas/{$agendada->id_cita}/confirmar", $manana)->assertStatus(409);
        $this->assertSame('ATENDIDA', $atendida->refresh()->estado);
        $this->assertSame('CANCELADA', $cancelada->refresh()->estado);
    }

    public function test_reprogramar_no_cambia_el_paciente(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $cita = $this->cita('AGENDADA');
        $otro = Paciente::create(['numero_expediente' => 'EXP-2026-000802', 'nombre_completo' => 'Otro', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);

        $this->patchJson("/api/v1/citas/{$cita->id_cita}/reprogramar", ['id_paciente' => $otro->id_paciente, 'id_medico' => $this->medico->id, 'fecha' => today()->addDay()->toDateString(), 'hora_inicio' => '09:00'])
            ->assertStatus(422);
        $this->patchJson("/api/v1/citas/{$cita->id_cita}/reprogramar", ['id_medico' => $this->medico->id, 'fecha' => today()->addDay()->toDateString(), 'hora_inicio' => '09:00'])
            ->assertOk()->assertJsonPath('data.hora_inicio', '09:00');
        $this->assertSame($this->paciente->id_paciente, $cita->refresh()->id_paciente);
    }

    public function test_consulta_solo_sobre_cita_vigente_y_una_vez(): void
    {
        Sanctum::actingAs($this->medico);
        $cancelada = $this->cita('CANCELADA');
        $enEspera = $this->cita('EN_ESPERA');

        $this->postJson("/api/v1/citas/{$cancelada->id_cita}/consulta")->assertStatus(409);
        $this->postJson("/api/v1/citas/{$enEspera->id_cita}/consulta")->assertCreated();
        $this->postJson("/api/v1/citas/{$enEspera->id_cita}/consulta")->assertStatus(409);
        $this->assertSame(1, Consulta::count());
        $this->assertSame('EN_ATENCION', $enEspera->refresh()->estado);
    }
}
