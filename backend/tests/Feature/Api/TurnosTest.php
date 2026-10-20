<?php

namespace Tests\Feature\Api;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class TurnosTest extends TestCase
{
    use RefreshDatabase;

    public function test_turno_solapado_responde_409(): void
    {
        $a = User::factory()->create(['rol' => 'ADMINISTRADOR']);
        $m = User::factory()->create(['rol' => 'MEDICO']);
        Sanctum::actingAs($a);
        $d = ['id_usuario' => $m->id, 'fecha' => '2026-10-20', 'hora_inicio' => '08:00', 'hora_fin' => '12:00', 'area' => 'Consulta general'];
        $this->postJson('/api/v1/turnos', $d)->assertCreated();
        $this->postJson('/api/v1/turnos', array_merge($d, ['hora_inicio' => '11:00', 'hora_fin' => '13:00']))->assertStatus(409);
    }

    public function test_no_se_asigna_turno_a_un_paciente(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'ADMINISTRADOR']));
        $paciente = User::factory()->create(['rol' => 'PACIENTE']);

        $this->postJson('/api/v1/turnos', ['id_usuario' => $paciente->id, 'fecha' => '2026-10-20', 'hora_inicio' => '08:00', 'hora_fin' => '12:00', 'area' => 'Consulta general'])
            ->assertStatus(422)->assertJsonValidationErrors('id_usuario');
    }
}
