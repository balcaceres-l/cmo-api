<?php

namespace Tests\Feature\Api;

use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ActivacionCuentaTest extends TestCase
{
    use RefreshDatabase;

    private Paciente $paciente;

    protected function setUp(): void
    {
        parent::setUp();
        // Paciente registrado por recepción, todavía sin cuenta.
        $this->paciente = Paciente::create(['numero_expediente' => 'EXP-2026-000301', 'nombre_completo' => 'Rosa Alvarado Mejía', 'fecha_nacimiento' => '1985-03-02', 'dui' => '01234567-8', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $this->paciente->id_paciente, 'antecedentes_personales' => 'Dato reservado']);
    }

    private function registrar(array $extra = [])
    {
        return $this->postJson('/api/v1/auth/registro', $extra + [
            'nombre_completo' => 'Otra Persona', 'usuario' => 'intruso', 'password' => 'Hospital123', 'password_confirmation' => 'Hospital123',
            'dui' => '01234567-8', 'fecha_nacimiento' => '1999-01-01', 'sexo' => 'M',
        ]);
    }

    public function test_conocer_el_dui_no_basta_para_tomar_un_expediente(): void
    {
        $this->registrar()->assertStatus(422)->assertJsonValidationErrors('codigo_activacion');
        $this->registrar(['codigo_activacion' => '123456'])->assertStatus(422);

        $this->assertNull($this->paciente->refresh()->id_usuario);
        $this->assertSame(0, User::where('usuario', 'intruso')->count());
    }

    public function test_con_el_codigo_de_recepcion_se_vincula_una_sola_vez(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $codigo = $this->postJson("/api/v1/pacientes/{$this->paciente->id_paciente}/codigo-activacion")
            ->assertCreated()->json('data.codigo');
        $this->app['auth']->forgetGuards();

        $this->registrar(['usuario' => 'rosa.alvarado', 'codigo_activacion' => $codigo])->assertCreated()
            ->assertJsonPath('data.id_paciente', $this->paciente->id_paciente);
        $this->registrar(['usuario' => 'otra.cuenta', 'codigo_activacion' => $codigo])->assertStatus(422);

        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $this->postJson("/api/v1/pacientes/{$this->paciente->id_paciente}/codigo-activacion")->assertStatus(409);
    }

    public function test_el_codigo_se_invalida_tras_cinco_intentos_fallidos(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $codigo = $this->postJson("/api/v1/pacientes/{$this->paciente->id_paciente}/codigo-activacion")->json('data.codigo');
        $this->app['auth']->forgetGuards();
        $incorrecto = $codigo === '000000' ? '111111' : '000000';

        foreach (range(1, 5) as $intento) {
            $this->registrar(['usuario' => "intento{$intento}", 'codigo_activacion' => $incorrecto])->assertStatus(422);
        }

        $this->registrar(['usuario' => 'tarde', 'codigo_activacion' => $codigo])->assertStatus(422);
        $this->assertNull($this->paciente->refresh()->id_usuario);
    }

    public function test_un_dui_nuevo_se_registra_sin_codigo(): void
    {
        $this->registrar(['dui' => '09876543-2', 'usuario' => 'nuevo.paciente'])->assertCreated();
    }
}
