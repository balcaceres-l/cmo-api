<?php

namespace Tests\Feature\Api;

use App\Exceptions\ConflictoException;
use App\Models\Cama;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use App\Services\AmbulanciaService;
use App\Services\AsignacionCamaService;
use Database\Seeders\AmbulanciaSeeder;
use Database\Seeders\HabitacionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ConcurrenciaTest extends TestCase
{
    use RefreshDatabase;

    public function test_segunda_asignacion_y_segundo_despacho_entran_en_conflicto(): void
    {
        $this->seed([HabitacionSeeder::class, AmbulanciaSeeder::class]);
        $medico = User::factory()->create(['rol' => 'MEDICO']);
        $pacientes = collect([1, 2])->map(function ($n) {
            $p = Paciente::create(['numero_expediente' => sprintf('EXP-2026-%06d', $n + 200), 'nombre_completo' => "P$n", 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
            HistorialClinico::create(['id_paciente' => $p->id_paciente]);

            return $p;
        });
        $cama = Cama::whereHas('habitacion', fn ($q) => $q->where('tipo', 'VIP'))->first();
        app(AsignacionCamaService::class)->asignar($pacientes[0], 'VIP', $cama->id_cama, $medico, 'Uno', $medico);
        try {
            app(AsignacionCamaService::class)->asignar($pacientes[1], 'VIP', $cama->id_cama, $medico, 'Dos', $medico);
            $this->fail('Debió producir conflicto');
        } catch (ConflictoException) {
            $this->assertTrue(true);
        }app(AmbulanciaService::class)->despachar(['id_solicitante' => $medico->id, 'paciente_nombre' => 'P', 'direccion' => 'D', 'descripcion' => 'E', 'prioridad' => 'ALTA']);
        $this->expectException(ConflictoException::class);
        app(AmbulanciaService::class)->despachar(['id_solicitante' => $medico->id, 'paciente_nombre' => 'P2', 'direccion' => 'D', 'descripcion' => 'E', 'prioridad' => 'ALTA']);
    }
}
