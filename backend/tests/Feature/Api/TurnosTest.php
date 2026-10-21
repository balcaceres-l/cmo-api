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

    public function test_turno_nocturno_se_guarda_en_dos_dias_y_cuenta_para_la_agenda(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'ADMINISTRADOR']));
        $m = User::factory()->create(['rol' => 'MEDICO']);
        $noche = ['id_usuario' => $m->id, 'fecha' => '2026-10-20', 'hora_inicio' => '22:00', 'hora_fin' => '06:00', 'area' => 'Emergencia'];

        $this->postJson('/api/v1/turnos', $noche)->assertCreated()
            ->assertJsonCount(2, 'data')
            ->assertJsonPath('data.0.fecha', '2026-10-20')
            ->assertJsonPath('data.1.fecha', '2026-10-21')
            ->assertJsonPath('data.1.hora_inicio', '00:00');
        // La segunda parte también se protege contra solapes.
        $this->postJson('/api/v1/turnos', ['fecha' => '2026-10-21', 'hora_inicio' => '05:00', 'hora_fin' => '09:00'] + $noche)->assertStatus(409);

        $madrugada = $this->getJson("/api/v1/agenda/disponibilidad?id_medico={$m->id}&fecha=2026-10-21")->assertOk()->json('data');
        $this->assertSame('00:00', $madrugada[0]['hora_inicio']);
        $this->assertCount(12, $madrugada);
    }

    public function test_al_editar_no_se_cruza_la_medianoche(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'ADMINISTRADOR']));
        $m = User::factory()->create(['rol' => 'MEDICO']);
        $d = ['id_usuario' => $m->id, 'fecha' => '2026-10-20', 'hora_inicio' => '08:00', 'hora_fin' => '12:00', 'area' => 'Consulta general'];
        $id = $this->postJson('/api/v1/turnos', $d)->json('data.0.id_turno');

        $this->putJson("/api/v1/turnos/{$id}", ['hora_inicio' => '22:00', 'hora_fin' => '06:00'] + $d)->assertStatus(422);
    }

    public function test_no_se_asigna_turno_a_un_paciente(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'ADMINISTRADOR']));
        $paciente = User::factory()->create(['rol' => 'PACIENTE']);

        $this->postJson('/api/v1/turnos', ['id_usuario' => $paciente->id, 'fecha' => '2026-10-20', 'hora_inicio' => '08:00', 'hora_fin' => '12:00', 'area' => 'Consulta general'])
            ->assertStatus(422)->assertJsonValidationErrors('id_usuario');
    }
}
