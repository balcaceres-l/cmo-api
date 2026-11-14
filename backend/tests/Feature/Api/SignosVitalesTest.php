<?php

namespace Tests\Feature\Api;

use App\Models\Cita;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class SignosVitalesTest extends TestCase
{
    use RefreshDatabase;

    private function cita(User $medico, string $estado = 'EN_ESPERA', $fecha = null): Cita
    {
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-'.str_pad((string) random_int(1, 999999), 6, '0', STR_PAD_LEFT), 'nombre_completo' => 'P', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);

        return Cita::create(['id_paciente' => $p->id_paciente, 'id_medico' => $medico->id, 'fecha' => $fecha ?? today(), 'hora_inicio' => '08:00', 'hora_fin' => '08:30', 'motivo' => 'Control', 'estado' => $estado, 'id_creado_por' => $medico->id]);
    }

    private array $signos = ['presion_sistolica' => 120, 'presion_diastolica' => 80, 'frecuencia_cardiaca' => 78, 'temperatura_c' => 36.6, 'saturacion_oxigeno' => 98, 'peso_kg' => 70.5, 'talla_cm' => 170];

    public function test_recepcion_registra_y_actualiza_signos_que_salen_en_la_lista_de_citas(): void
    {
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $cita = $this->cita($medico);
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));

        $this->putJson("/api/v1/citas/{$cita->id_cita}/signos-vitales", $this->signos)->assertOk()
            ->assertJsonPath('data.presion_sistolica', 120)
            ->assertJsonPath('data.temperatura_c', 36.6);
        // Volver a tomarlos reemplaza los anteriores: sigue habiendo un solo registro.
        $this->putJson("/api/v1/citas/{$cita->id_cita}/signos-vitales", ['frecuencia_cardiaca' => 90])->assertOk();
        $this->assertSame(1, $cita->signosVitales()->count());

        $this->getJson('/api/v1/citas')->assertOk()
            ->assertJsonPath('data.data.0.signos_vitales.frecuencia_cardiaca', 90);
        // El cambio de estado no lo hacen los signos: la llegada se registra aparte.
        $this->assertSame('EN_ESPERA', $cita->refresh()->estado);
    }

    public function test_reglas_de_dia_estado_medico_y_valores(): void
    {
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));

        $manana = $this->cita($medico, 'AGENDADA', today()->addDay());
        $this->putJson("/api/v1/citas/{$manana->id_cita}/signos-vitales", $this->signos)->assertStatus(409);

        $atendida = $this->cita($medico, 'ATENDIDA');
        $this->putJson("/api/v1/citas/{$atendida->id_cita}/signos-vitales", $this->signos)->assertStatus(409);

        $hoy = $this->cita($medico);
        $this->putJson("/api/v1/citas/{$hoy->id_cita}/signos-vitales", [])->assertStatus(422);
        $this->putJson("/api/v1/citas/{$hoy->id_cita}/signos-vitales", ['presion_sistolica' => 80, 'presion_diastolica' => 90])
            ->assertStatus(422)->assertJsonValidationErrors('presion_diastolica');
        $this->putJson("/api/v1/citas/{$hoy->id_cita}/signos-vitales", ['saturacion_oxigeno' => 120])
            ->assertStatus(422)->assertJsonValidationErrors('saturacion_oxigeno');

        // Otro médico no registra signos en una cita ajena; el de la cita sí.
        Sanctum::actingAs(User::factory()->create(['rol' => 'MEDICO']));
        $this->putJson("/api/v1/citas/{$hoy->id_cita}/signos-vitales", $this->signos)->assertForbidden();
        Sanctum::actingAs($medico);
        $this->putJson("/api/v1/citas/{$hoy->id_cita}/signos-vitales", $this->signos)->assertOk();

        Sanctum::actingAs(User::factory()->create(['rol' => 'PACIENTE']));
        $this->putJson("/api/v1/citas/{$hoy->id_cita}/signos-vitales", $this->signos)->assertForbidden();
    }
}
