<?php

namespace Tests\Feature\Api;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class UsuarioInactivoTest extends TestCase
{
    use RefreshDatabase;

    public function test_desactivar_revoca_los_tokens_existentes(): void
    {
        $admin = User::factory()->create(['rol' => 'ADMINISTRADOR']);
        $recepcion = User::factory()->create(['rol' => 'RECEPCIONISTA']);
        $token = $recepcion->createToken('movil')->plainTextToken;

        $this->withToken($admin->createToken('web')->plainTextToken)
            ->patchJson("/api/v1/usuarios/{$recepcion->id}/estado", ['estado' => 'INACTIVO'])
            ->assertOk();
        $this->app['auth']->forgetGuards();

        $this->withToken($token)->getJson('/api/v1/recursos')->assertUnauthorized();
        $this->assertSame(0, $recepcion->tokens()->count());
    }

    public function test_token_de_usuario_inactivo_es_rechazado(): void
    {
        // Cubre cuentas desactivadas por otra vía (por ejemplo, directo en la base).
        $u = User::factory()->create(['rol' => 'RECEPCIONISTA']);
        $token = $u->createToken('movil')->plainTextToken;
        $u->update(['estado' => 'INACTIVO']);

        $this->withToken($token)->getJson('/api/v1/auth/me')
            ->assertUnauthorized()
            ->assertJsonPath('message', 'El usuario se encuentra inactivo.');
    }
}
