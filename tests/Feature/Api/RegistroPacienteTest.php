<?php

namespace Tests\Feature\Api;

use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class RegistroPacienteTest extends TestCase
{
    use RefreshDatabase;

    public function test_registro_crea_usuario_paciente_e_historial(): void
    {
        $r = $this->postJson('/api/v1/auth/registro', ['nombre_completo' => 'Ana Pérez', 'usuario' => 'ana', 'email' => 'ana@example.com', 'password' => 'Hospital2026!', 'password_confirmation' => 'Hospital2026!', 'dui' => '01234567-8', 'fecha_nacimiento' => '1995-05-20', 'sexo' => 'F']);
        $r->assertCreated()->assertJsonPath('data.usuario.rol', 'PACIENTE');
        $this->assertCount(1, User::where('rol', 'PACIENTE')->get());
        $this->assertCount(1, Paciente::all());
        $this->assertCount(1, HistorialClinico::all());
    }

    public function test_dui_con_cuenta_responde_422(): void
    {
        $payload = ['nombre_completo' => 'Ana Pérez', 'usuario' => 'ana', 'password' => 'Hospital2026!', 'password_confirmation' => 'Hospital2026!', 'dui' => '01234567-8', 'fecha_nacimiento' => '1995-05-20', 'sexo' => 'F'];
        $this->postJson('/api/v1/auth/registro', $payload)->assertCreated();
        $this->postJson('/api/v1/auth/registro', array_merge($payload, ['usuario' => 'ana2']))->assertUnprocessable();
    }
}
