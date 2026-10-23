<?php

namespace Tests\Feature\Api;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AuthTest extends TestCase
{
    use RefreshDatabase;

    public function test_login_devuelve_token_y_rechaza_credenciales_invalidas(): void
    {
        User::factory()->create(['usuario' => 'recepcion', 'password' => 'Secreta123', 'rol' => 'RECEPCIONISTA']);
        User::factory()->create(['usuario' => 'inactivo', 'password' => 'Secreta123', 'estado' => 'INACTIVO']);

        $this->postJson('/api/v1/auth/login', ['usuario' => 'recepcion', 'password' => 'Secreta123'])
            ->assertOk()
            ->assertJsonPath('data.usuario.rol', 'RECEPCIONISTA')
            ->assertJsonPath('data.token_type', 'Bearer')
            ->assertJsonMissingPath('data.usuario.password');
        $this->postJson('/api/v1/auth/login', ['usuario' => 'recepcion', 'password' => 'otra'])->assertUnauthorized();
        $this->postJson('/api/v1/auth/login', ['usuario' => 'inactivo', 'password' => 'Secreta123'])->assertUnauthorized();
    }

    public function test_me_y_logout_revocan_el_token_actual(): void
    {
        $u = User::factory()->create(['rol' => 'MEDICO']);
        $token = $u->createToken('movil')->plainTextToken;

        $this->withToken($token)->getJson('/api/v1/auth/me')->assertOk()->assertJsonPath('data.usuario.id', $u->id);
        $this->withToken($token)->postJson('/api/v1/auth/logout')->assertOk();
        $this->app['auth']->forgetGuards();
        $this->withToken($token)->getJson('/api/v1/auth/me')->assertUnauthorized();
    }

    public function test_cambio_de_contrasena_exige_la_actual_y_cierra_sesiones(): void
    {
        $u = User::factory()->create(['usuario' => 'medico', 'password' => 'Anterior123', 'rol' => 'MEDICO']);
        $otroDispositivo = $u->createToken('tablet')->plainTextToken;
        $token = $u->createToken('movil')->plainTextToken;

        $this->withToken($token)->patchJson('/api/v1/auth/password', ['password_actual' => 'equivocada', 'password' => 'Nueva12345', 'password_confirmation' => 'Nueva12345'])
            ->assertStatus(422);
        $this->withToken($token)->patchJson('/api/v1/auth/password', ['password_actual' => 'Anterior123', 'password' => 'Nueva12345', 'password_confirmation' => 'Nueva12345'])
            ->assertOk();
        $this->app['auth']->forgetGuards();

        $this->withToken($otroDispositivo)->getJson('/api/v1/auth/me')->assertUnauthorized();
        $this->postJson('/api/v1/auth/login', ['usuario' => 'medico', 'password' => 'Nueva12345'])->assertOk();
    }
}
