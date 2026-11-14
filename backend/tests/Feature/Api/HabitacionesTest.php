<?php

namespace Tests\Feature\Api;

use App\Exceptions\ConflictoException;
use App\Models\Cama;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use App\Services\AsignacionCamaService;
use Database\Seeders\HabitacionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class HabitacionesTest extends TestCase
{
    use RefreshDatabase;

    public function test_capacidad_sembrada_es_exacta_y_conflictos(): void
    {
        $this->seed(HabitacionSeeder::class);
        $this->assertSame(5, Cama::whereHas('habitacion', fn ($q) => $q->where('tipo', 'VIP'))->count());
        $this->assertSame(12, Cama::whereHas('habitacion', fn ($q) => $q->where('tipo', 'SEMIPRIVADA'))->count());
        $u = User::factory()->create(['rol' => 'MEDICO']);
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-999991', 'nombre_completo' => 'P', 'fecha_nacimiento' => '2000-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $s = app(AsignacionCamaService::class);
        $c = Cama::whereHas('habitacion', fn ($q) => $q->where('tipo', 'VIP'))->first();
        $s->asignar($p, 'VIP', $c->id_cama, $u, 'Prueba', $u);
        $this->expectException(ConflictoException::class);
        $s->asignar($p, 'VIP', null, $u, 'Duplicado', $u);
    }

    public function test_cama_de_otro_tipo_y_medico_invalido_responden_422(): void
    {
        $this->seed(HabitacionSeeder::class);
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-999992', 'nombre_completo' => 'P', 'fecha_nacimiento' => '2000-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $vip = Cama::where('codigo', 'VIP-301')->first();
        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $datos = ['id_paciente' => $p->id_paciente, 'tipo' => 'SEMIPRIVADA', 'id_medico' => $medico->id, 'motivo' => 'Ingreso'];

        $this->postJson('/api/v1/ingresos', $datos + ['id_cama' => $vip->id_cama])->assertStatus(422)->assertJsonValidationErrors('id_cama');
        $this->postJson('/api/v1/ingresos', ['id_medico' => User::factory()->create(['rol' => 'RECEPCIONISTA'])->id] + $datos)
            ->assertStatus(422)->assertJsonValidationErrors('id_medico');
        $this->postJson('/api/v1/ingresos', $datos)->assertCreated();
    }

    public function test_permisos_de_camas_entre_admin_general_jefe_operaciones_y_medico(): void
    {
        $this->seed(HabitacionSeeder::class);
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $otroMedico = User::factory()->create(['rol' => 'MEDICO']);
        $adminGeneral = User::factory()->create(['rol' => 'ADMINISTRADOR', 'cargo' => 'Administración']);
        $jefeOps = User::factory()->create(['rol' => 'ADMINISTRADOR', 'cargo' => 'Jefe de Operaciones']);

        $p = Paciente::create(['numero_expediente' => 'EXP-2026-999993', 'nombre_completo' => 'Paciente Prueba', 'fecha_nacimiento' => '2000-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);

        $cama1 = Cama::where('codigo', 'VIP-301')->firstOrFail();
        $cama2 = Cama::where('codigo', 'VIP-302')->firstOrFail();

        $payload = [
            'id_paciente' => $p->id_paciente,
            'tipo' => 'VIP',
            'id_cama' => $cama1->id_cama,
            'id_medico' => $medico->id,
            'motivo' => 'Observación postoperatoria',
        ];

        // Admin General no puede operar camas (solo lectura)
        Sanctum::actingAs($adminGeneral);
        $this->getJson('/api/v1/habitaciones')->assertOk();
        $this->getJson('/api/v1/ingresos')->assertOk();
        $this->postJson('/api/v1/ingresos', $payload)->assertForbidden();

        // Jefe de Operaciones sí puede ingresar, trasladar y dar de alta
        Sanctum::actingAs($jefeOps);
        $resIngreso = $this->postJson('/api/v1/ingresos', $payload)->assertCreated();
        $idIngreso = $resIngreso->json('data.id_ingreso');

        // Médico ajeno ve la cama ocupada pero paciente/ingreso ocultos (privacidad iOS)
        Sanctum::actingAs($otroMedico);
        $resHabMedico = $this->getJson('/api/v1/habitaciones')->assertOk();
        $vip301 = collect($resHabMedico->json('data.3'))->flatMap(fn ($h) => $h['camas'])->firstWhere('codigo', 'VIP-301');
        $this->assertSame('OCUPADA', $vip301['estado']);
        $this->assertNotEmpty($vip301['id_cama']);
        $this->assertNull($vip301['paciente']);
        $this->assertNull($vip301['ingreso']);

        // Admin General tampoco puede trasladar ni dar de alta
        Sanctum::actingAs($adminGeneral);
        $this->patchJson("/api/v1/ingresos/{$idIngreso}/trasladar", ['id_cama' => $cama2->id_cama])->assertForbidden();
        $this->patchJson("/api/v1/ingresos/{$idIngreso}/alta")->assertForbidden();

        // Jefe de Operaciones traslada y da de alta correctamente
        Sanctum::actingAs($jefeOps);
        $this->patchJson("/api/v1/ingresos/{$idIngreso}/trasladar", ['id_cama' => $cama2->id_cama])->assertOk();
        $this->patchJson("/api/v1/ingresos/{$idIngreso}/alta")->assertOk();
    }

    public function test_mapa_de_camas_incluye_ids_para_operar_desde_las_apps(): void
    {
        $this->seed(HabitacionSeeder::class);
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-999994', 'nombre_completo' => 'P', 'fecha_nacimiento' => '2000-01-01', 'sexo' => 'F', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente]);
        $vip = Cama::where('codigo', 'VIP-301')->first();
        $ingreso = app(AsignacionCamaService::class)->asignar($p, 'VIP', $vip->id_cama, $medico, 'Prueba', $medico);

        Sanctum::actingAs(User::factory()->create(['rol' => 'RECEPCIONISTA']));
        $cama = collect($this->getJson('/api/v1/habitaciones')->assertOk()->json('data'))->flatten(1)
            ->flatMap(fn ($h) => $h['camas'])->firstWhere('codigo', 'VIP-301');
        $this->assertSame($vip->id_cama, $cama['id_cama']);
        $this->assertSame($ingreso->id_ingreso, $cama['id_ingreso']);
        $this->assertSame($ingreso->id_ingreso, $cama['ingreso']['id_ingreso']);

        // Un médico que no es el tratante ve la cama ocupada, pero no el ingreso ni el paciente.
        Sanctum::actingAs(User::factory()->create(['rol' => 'MEDICO']));
        $cama = collect($this->getJson('/api/v1/habitaciones')->json('data'))->flatten(1)
            ->flatMap(fn ($h) => $h['camas'])->firstWhere('codigo', 'VIP-301');
        $this->assertSame('OCUPADA', $cama['estado']);
        $this->assertNull($cama['id_ingreso']);
        $this->assertNull($cama['ingreso']);
        $this->assertNull($cama['paciente']);
    }
}
