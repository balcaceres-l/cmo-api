<?php

namespace Tests\Feature\Api;

use App\Models\Consultorio;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\TurnoPersonal;
use App\Models\User;
use Database\Seeders\ConsultorioSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ConsultorioTest extends TestCase
{
    use RefreshDatabase;

    private string $fecha;

    private Paciente $paciente;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(ConsultorioSeeder::class);
        $this->fecha = today()->addDay()->toDateString();
        $this->paciente = Paciente::create(['numero_expediente' => 'EXP-2026-000951', 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $this->paciente->id_paciente]);
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
    }

    private function medico(): User
    {
        $m = User::factory()->create(['rol' => 'MEDICO']);
        TurnoPersonal::create(['id_usuario' => $m->id, 'fecha' => $this->fecha, 'hora_inicio' => '08:00', 'hora_fin' => '12:00', 'area' => 'Consulta general']);

        return $m;
    }

    public function test_la_planta_uno_tiene_consultorios_de_consulta_general(): void
    {
        $this->getJson('/api/v1/consultorios')->assertOk()
            ->assertJsonCount(4, 'data')
            ->assertJsonPath('data.0.codigo', 'C-101')
            ->assertJsonPath('data.0.planta', 1);
    }

    public function test_la_cita_indica_su_consultorio_y_no_se_duplica(): void
    {
        $c101 = Consultorio::where('codigo', 'C-101')->value('id_consultorio');
        $cita = ['id_paciente' => $this->paciente->id_paciente, 'fecha' => $this->fecha, 'hora_inicio' => '08:00', 'motivo' => 'Control', 'id_consultorio' => $c101];

        $this->postJson('/api/v1/citas', ['id_medico' => $this->medico()->id] + $cita)->assertCreated()
            ->assertJsonPath('data.id_consultorio', $c101);
        // Otro médico, misma hora, mismo consultorio: el consultorio está ocupado.
        $this->postJson('/api/v1/citas', ['id_medico' => $this->medico()->id] + $cita)->assertStatus(409)
            ->assertJsonPath('message', 'El consultorio ya está ocupado a esa hora.');

        $this->getJson('/api/v1/citas')->assertOk()->assertJsonPath('data.data.0.consultorio.codigo', 'C-101');
        $this->postJson('/api/v1/citas', ['id_medico' => $this->medico()->id, 'id_consultorio' => 'no-existe'] + $cita)->assertStatus(422);
    }
}
