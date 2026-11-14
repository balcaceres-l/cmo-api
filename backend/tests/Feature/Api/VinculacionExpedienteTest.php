<?php

namespace Tests\Feature\Api;

use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use App\Services\ResendMailService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Laravel\Sanctum\Sanctum;
use Mockery;
use Tests\TestCase;

class VinculacionExpedienteTest extends TestCase
{
    use RefreshDatabase;

    private Paciente $paciente;
    private User $usuarioSinExpediente;

    protected function setUp(): void
    {
        parent::setUp();

        // Paciente registrado en el hospital sin cuenta
        $this->paciente = Paciente::create([
            'numero_expediente' => 'EXP-2026-000501',
            'nombre_completo' => 'Carlos Mendoza',
            'fecha_nacimiento' => '1992-04-10',
            'dui' => '05554443-2',
            'sexo' => 'M',
            'fecha_registro' => now(),
        ]);
        HistorialClinico::create(['id_paciente' => $this->paciente->id_paciente]);

        // Usuario registrado como PACIENTE con email pero sin expediente enlazado
        $this->usuarioSinExpediente = User::create([
            'nombre_completo' => 'Carlos Mendoza',
            'usuario' => 'carlos.mendoza',
            'email' => 'carlos@ejemplo.com',
            'password' => 'Hospital2026!',
            'rol' => 'PACIENTE',
            'estado' => 'ACTIVO',
        ]);
    }

    public function test_cuenta_sin_email_no_puede_solicitar_otp(): void
    {
        $sinEmail = User::create([
            'nombre_completo' => 'Sin Email',
            'usuario' => 'sin.email',
            'email' => null,
            'password' => 'Hospital2026!',
            'rol' => 'PACIENTE',
            'estado' => 'ACTIVO',
        ]);
        Sanctum::actingAs($sinEmail);

        $this->postJson('/api/v1/mi/expediente/solicitar-otp', ['dui' => '05554443-2'])
            ->assertStatus(422)
            ->assertJsonPath('success', false)
            ->assertJsonValidationErrors('email');
    }

    public function test_cuenta_con_expediente_previo_es_rechazada(): void
    {
        $this->paciente->update(['id_usuario' => $this->usuarioSinExpediente->id]);
        Sanctum::actingAs($this->usuarioSinExpediente);

        $this->postJson('/api/v1/mi/expediente/solicitar-otp', ['dui' => '05554443-2'])
            ->assertStatus(409)
            ->assertJsonPath('success', false);
    }

    public function test_solicitar_otp_exitoso_y_confirmar_vinculacion(): void
    {
        // Mock de ResendMailService para evitar llamadas HTTP reales en pruebas
        $mockResend = Mockery::mock(ResendMailService::class);
        $mockResend->shouldReceive('enviarOtp')
            ->once()
            ->andReturn(true);
        $this->app->instance(ResendMailService::class, $mockResend);

        Sanctum::actingAs($this->usuarioSinExpediente);

        // 1. Solicitar OTP
        $res = $this->postJson('/api/v1/mi/expediente/solicitar-otp', ['dui' => '05554443-2'])
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonPath('data.email_enmascarado', 'c****s@ejemplo.com');

        // Extraer clave de cache para obtener el OTP generado
        $clave = "otp-vincular:{$this->usuarioSinExpediente->id}";
        $this->assertTrue(Cache::has($clave));

        // 2. Intentar confirmar con OTP incorrecto
        $this->postJson('/api/v1/mi/expediente/confirmar-otp', [
            'dui' => '05554443-2',
            'otp' => '000000',
        ])->assertStatus(422)->assertJsonValidationErrors('otp');

        // 3. Confirmar con OTP correcto (obtenido simulando que el usuario lo lee de su correo)
        // El test simula la verificación directa
        $otpSimulado = '654321';
        Cache::put($clave, [
            'otp' => bcrypt($otpSimulado),
            'dui' => '05554443-2',
            'id_paciente' => $this->paciente->id_paciente,
            'intentos' => 0,
        ], now()->addMinutes(15));

        $confirmacion = $this->postJson('/api/v1/mi/expediente/confirmar-otp', [
            'dui' => '05554443-2',
            'otp' => $otpSimulado,
        ])->assertOk()
          ->assertJsonPath('success', true)
          ->assertJsonPath('data.id_paciente', $this->paciente->id_paciente)
          ->assertJsonPath('data.numero_expediente', 'EXP-2026-000501');

        // Verificar que el expediente quedó enlazado al usuario
        $this->paciente->refresh();
        $this->assertSame($this->usuarioSinExpediente->id, $this->paciente->id_usuario);
        $this->assertFalse(Cache::has($clave), 'El OTP se elimina tras ser usado');
    }
}
