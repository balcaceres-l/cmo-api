<?php

namespace Tests\Feature\Api;

use App\Models\Especialidad;
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
        $medico->refresh();
        $this->assertTrue(Hash::check('Original123', $medico->password));
        $this->assertNull($medico->cargo);
        $this->assertSame('Emergencia', $medico->especialidad_nombre);
    }

    public function test_vigencia_rotacion_bloquea_turnos_fuera_de_rango(): void
    {
        Sanctum::actingAs($this->admin);
        $ingreso = today()->format('Y-m-d');
        $salida = today()->addDays(5)->format('Y-m-d');
        $fuera = today()->addDays(10)->format('Y-m-d');

        $res = $this->postJson('/api/v1/usuarios', [
            'nombre_completo' => 'Dr. Residente Rotante',
            'usuario' => 'dr.rotante',
            'password' => 'Hospital123',
            'rol' => 'MEDICO',
            'cargo' => 'Emergencia',
            'fecha_ingreso' => $ingreso,
            'fecha_salida' => $salida,
        ])->assertCreated();

        $idMedico = $res->json('data.id');

        // Dentro de vigencia: permitido
        $this->postJson('/api/v1/turnos', [
            'id_usuario' => $idMedico,
            'fecha' => $salida,
            'hora_inicio' => '08:00',
            'hora_fin' => '16:00',
            'area' => 'Servicio de Emergencias',
        ])->assertCreated();

        // Después de fecha_salida: bloqueado con 422
        $this->postJson('/api/v1/turnos', [
            'id_usuario' => $idMedico,
            'fecha' => $fuera,
            'hora_inicio' => '08:00',
            'hora_fin' => '16:00',
            'area' => 'Servicio de Emergencias',
        ])->assertStatus(422);
    }

    public function test_cargos_por_rol_y_especialidad_para_medico(): void
    {
        Sanctum::actingAs($this->admin);
        $pediatria = Especialidad::where('nombre', 'Pediatría')->firstOrFail();

        $resMedico = $this->postJson('/api/v1/usuarios', [
            'nombre_completo' => 'Dr. Pediatra',
            'usuario' => 'dr.pediatra',
            'password' => 'Hospital123',
            'rol' => 'MEDICO',
            'id_especialidad' => $pediatria->id_especialidad,
        ])->assertCreated()
            ->assertJsonPath('data.cargo', null)
            ->assertJsonPath('data.especialidad_nombre', 'Pediatría');

        $medico = User::findOrFail($resMedico->json('data.id'));
        $this->assertNull($medico->cargo);
        $this->assertSame('Pediatría', $medico->especialidad_nombre);

        $this->postJson('/api/v1/usuarios', [
            'nombre_completo' => 'Personal Inválido',
            'usuario' => 'recep.invalida',
            'password' => 'Hospital123',
            'rol' => 'RECEPCIONISTA',
            'cargo' => 'Cirujano',
        ])->assertStatus(422)->assertJsonValidationErrors('cargo');

        $resEnfermera = $this->postJson('/api/v1/usuarios', [
            'nombre_completo' => 'Enfermera Turno',
            'usuario' => 'enf.turno',
            'password' => 'Hospital123',
            'rol' => 'RECEPCIONISTA',
            'cargo' => 'Enfermera',
        ])->assertCreated()
            ->assertJsonPath('data.cargo', 'Enfermera')
            ->assertJsonPath('data.id_especialidad', null);

        $idEnfermera = $resEnfermera->json('data.id');
        $this->putJson("/api/v1/usuarios/{$idEnfermera}", ['rol' => 'MEDICO'])->assertStatus(422);
    }

    public function test_crud_especialidades_protegidas_y_permisos(): void
    {
        Sanctum::actingAs($this->admin);

        $creada = $this->postJson('/api/v1/especialidades', [
            'nombre' => 'Neurocirugía',
            'descripcion' => 'Atención neuroquirúrgica',
        ])->assertCreated()
            ->assertJsonPath('data.nombre', 'Neurocirugía')
            ->json('data');

        $this->putJson("/api/v1/especialidades/{$creada['id_especialidad']}", [
            'descripcion' => 'Cirugía de sistema nervioso central',
        ])->assertOk()
            ->assertJsonPath('data.descripcion', 'Cirugía de sistema nervioso central');

        $this->deleteJson("/api/v1/especialidades/{$creada['id_especialidad']}")->assertOk();

        $emergencia = Especialidad::where('nombre', 'Emergencia')->firstOrFail();
        $this->deleteJson("/api/v1/especialidades/{$emergencia->id_especialidad}")->assertStatus(422);

        $jefeOperaciones = User::factory()->create([
            'rol' => 'ADMINISTRADOR',
            'cargo' => 'Jefe de Operaciones',
        ]);
        Sanctum::actingAs($jefeOperaciones);
        $this->postJson('/api/v1/especialidades', [
            'nombre' => 'Oftalmología',
        ])->assertForbidden();
    }
}
