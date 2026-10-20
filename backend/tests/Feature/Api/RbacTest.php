<?php

namespace Tests\Feature\Api;

use App\Models\User;
use Database\Seeders\AmbulanciaSeeder;
use Database\Seeders\HabitacionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class RbacTest extends TestCase
{
    use RefreshDatabase;

    public function test_matriz_basica_por_modulo(): void
    {
        $this->seed([HabitacionSeeder::class, AmbulanciaSeeder::class]);
        $casos = [['ADMINISTRADOR', '/api/v1/dashboard/metricas', 200], ['MEDICO', '/api/v1/dashboard/metricas', 403], ['RECEPCIONISTA', '/api/v1/recursos', 200], ['PACIENTE', '/api/v1/recursos', 403], ['MEDICO', '/api/v1/habitaciones', 200], ['PACIENTE', '/api/v1/habitaciones', 403]];
        foreach ($casos as [$rol,$ruta,$estado]) {
            Sanctum::actingAs(User::factory()->create(['rol' => $rol]));
            $this->getJson($ruta)->assertStatus($estado);
        }
    }
}
