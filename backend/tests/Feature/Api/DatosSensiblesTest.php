<?php

namespace Tests\Feature\Api;

use App\Models\Cita;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class DatosSensiblesTest extends TestCase
{
    use RefreshDatabase;

    private Paciente $paciente;

    private User $medico;

    protected function setUp(): void
    {
        parent::setUp();
        $cuenta = User::factory()->create(['rol' => 'PACIENTE']);
        $this->paciente = Paciente::create(['id_usuario' => $cuenta->id, 'numero_expediente' => 'EXP-2026-000601', 'nombre_completo' => 'Con Alergias', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'tipo_sangre' => 'O+', 'alergias' => 'Penicilina', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $this->paciente->id_paciente]);
        $this->medico = User::factory()->create(['rol' => 'MEDICO']);
        Cita::create(['id_paciente' => $this->paciente->id_paciente, 'id_medico' => $this->medico->id, 'fecha' => today(), 'hora_inicio' => '08:00', 'hora_fin' => '08:30', 'motivo' => 'Control', 'estado' => 'AGENDADA', 'id_creado_por' => $this->medico->id]);
    }

    public function test_recepcion_no_ve_tipo_de_sangre_ni_alergias(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $id = $this->paciente->id_paciente;

        $this->getJson("/api/v1/pacientes/{$id}")->assertOk()
            ->assertJsonPath('data.nombre_completo', 'Con Alergias')
            ->assertJsonMissingPath('data.alergias')
            ->assertJsonMissingPath('data.tipo_sangre');
        $this->getJson('/api/v1/pacientes')->assertOk()->assertDontSee('Penicilina');
        // Anidado dentro de las citas tampoco debe filtrarse.
        $this->getJson('/api/v1/citas')->assertOk()
            ->assertJsonPath('data.data.0.paciente.nombre_completo', 'Con Alergias')
            ->assertDontSee('Penicilina');
    }

    public function test_tratante_admin_y_el_propio_paciente_si_lo_ven(): void
    {
        $id = $this->paciente->id_paciente;
        foreach ([$this->medico, User::factory()->create(['rol' => 'ADMINISTRADOR'])] as $u) {
            Sanctum::actingAs($u);
            $this->getJson("/api/v1/pacientes/{$id}")->assertOk()->assertJsonPath('data.alergias', 'Penicilina');
        }
        Sanctum::actingAs($this->paciente->usuario);
        $this->getJson('/api/v1/mi/expediente')->assertOk()->assertJsonPath('data.tipo_sangre', 'O+')
            // Del médico, el paciente solo ve nombre y cargo.
            ->assertJsonPath('data.citas.0.medico.nombre_completo', $this->medico->nombre_completo)
            ->assertJsonMissingPath('data.citas.0.medico.email')
            ->assertJsonMissingPath('data.citas.0.medico.usuario')
            ->assertJsonMissingPath('data.citas.0.medico.telefono');
    }
}
