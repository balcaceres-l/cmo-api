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
}
