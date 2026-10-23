<?php

namespace Tests\Feature\Api;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class UsuariosTest extends TestCase
{
    use RefreshDatabase;

    private User $admin;

    protected function setUp(): void
    {
        parent::setUp();
        $this->admin = User::factory()->create(['rol' => 'ADMINISTRADOR']);
    }

    public function test_admin_crea_personal_pero_no_pacientes(): void
    {
        Sanctum::actingAs($this->admin);
        $datos = ['nombre_completo' => 'Dra. Nueva', 'usuario' => 'dra.nueva', 'password' => 'Hospital123', 'rol' => 'MEDICO', 'cargo' => 'Alergista'];

        $this->postJson('/api/v1/usuarios', $datos)->assertCreated()->assertJsonPath('data.rol', 'MEDICO');
        $this->postJson('/api/v1/usuarios', ['usuario' => 'paciente.x', 'rol' => 'PACIENTE'] + $datos)->assertStatus(422);

        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $this->postJson('/api/v1/usuarios', ['usuario' => 'otro'] + $datos)->assertForbidden();
    }

    public function test_admin_no_puede_desactivarse_ni_cambiar_su_rol(): void
    {
        Sanctum::actingAs($this->admin);

        $this->patchJson("/api/v1/usuarios/{$this->admin->id}/estado", ['estado' => 'INACTIVO'])->assertStatus(422);
        $this->putJson("/api/v1/usuarios/{$this->admin->id}", ['rol' => 'MEDICO'])->assertStatus(422);
        $this->putJson("/api/v1/usuarios/{$this->admin->id}", ['estado' => 'INACTIVO'])->assertStatus(422);
        $this->putJson("/api/v1/usuarios/{$this->admin->id}", ['telefono' => '2222-0000'])->assertOk();
        $this->assertSame('ACTIVO', $this->admin->refresh()->estado);
    }

    public function test_cuentas_de_paciente_no_se_editan_desde_usuarios(): void
    {
        Sanctum::actingAs($this->admin);
        $paciente = User::factory()->create(['rol' => 'PACIENTE']);

        $this->putJson("/api/v1/usuarios/{$paciente->id}", ['rol' => 'MEDICO'])->assertStatus(422);
        $this->assertSame('PACIENTE', $paciente->refresh()->rol);
    }

    public function test_editar_sin_password_conserva_la_actual(): void
    {
        Sanctum::actingAs($this->admin);
        $medico = User::factory()->create(['rol' => 'MEDICO', 'password' => 'Original123']);

        $this->putJson("/api/v1/usuarios/{$medico->id}", ['cargo' => 'Emergencia', 'password' => null])->assertOk();
        $this->assertTrue(Hash::check('Original123', $medico->refresh()->password));
        $this->assertSame('Emergencia', $medico->cargo);
    }
}
