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

class PrivacidadRevisionTest extends TestCase
{
    use RefreshDatabase;

    private User $medico;

    private User $cuenta;

    private OrdenLaboratorio $orden;

    protected function setUp(): void
    {
        parent::setUp();
        $this->medico = User::factory()->create(['rol' => 'MEDICO', 'usuario' => 'medico.privado', 'email' => 'medico@hospital.edu.sv', 'telefono' => '7777-0000', 'cargo' => 'Medicina general']);
        $this->cuenta = User::factory()->create(['rol' => 'PACIENTE']);
        $p = Paciente::create(['id_usuario' => $this->cuenta->id, 'numero_expediente' => 'EXP-2026-000401', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $cita = Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $this->medico->id, 'fecha' => today(), 'hora_inicio' => '08:00', 'hora_fin' => '08:30', 'motivo' => 'x', 'estado' => 'EN_ATENCION', 'id_creado_por' => $this->medico->id]);
        $consulta = Consulta::create(['id_cita' => $cita->id_cita, 'id_medico' => $this->medico->id, 'fecha_hora_inicio' => now()]);
        $examen = ExamenCatalogo::create(['nombre' => 'Hemograma', 'activo' => true]);
        $this->orden = OrdenLaboratorio::create(['id_consulta' => $consulta->id_consulta, 'id_paciente' => $p->id_paciente, 'id_examen' => $examen->id_examen, 'id_medico' => $this->medico->id, 'fecha_solicitud' => now(), 'estado' => 'EN_PROCESO']);
    }

    public function test_el_paciente_no_ve_ordenes_de_otros(): void
    {
        $otra = User::factory()->create(['rol' => 'PACIENTE']);
        $p = Paciente::create(['id_usuario' => $otra->id, 'numero_expediente' => 'EXP-2026-000402', 'nombre_completo' => 'Otro', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);

        Sanctum::actingAs($otra);
        $this->getJson('/api/v1/mi/laboratorio')->assertOk()->assertJsonPath('data.total', 0);
        Sanctum::actingAs($this->cuenta);
        $this->getJson('/api/v1/mi/laboratorio')->assertOk()->assertJsonPath('data.data.0.id_orden', $this->orden->id_orden);
    }

    public function test_el_paciente_solo_ve_nombre_y_cargo_del_medico(): void
    {
        Sanctum::actingAs($this->cuenta);

        $this->getJson('/api/v1/mi/citas')->assertOk()
            ->assertJsonPath('data.data.0.medico.nombre_completo', $this->medico->nombre_completo)
            ->assertJsonPath('data.data.0.medico.cargo', 'Medicina general')
            ->assertJsonMissingPath('data.data.0.medico.usuario')
            ->assertJsonMissingPath('data.data.0.medico.email')
            ->assertJsonMissingPath('data.data.0.medico.telefono');
    }
}
