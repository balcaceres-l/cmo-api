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
}
