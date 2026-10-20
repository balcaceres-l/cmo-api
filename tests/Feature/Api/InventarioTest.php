<?php

namespace Tests\Feature\Api;

use App\Models\Recurso;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class InventarioTest extends TestCase
{
    use RefreshDatabase;

    public function test_salida_negativa_responde_409(): void
    {
        $u = User::factory()->create(['rol' => 'ADMINISTRADOR']);
        $r = Recurso::create(['nombre' => 'Gasas', 'categoria' => 'Insumo', 'unidad' => 'paquete', 'cantidad' => 1, 'stock_minimo' => 1]);
        Sanctum::actingAs($u);
        $this->postJson("/api/v1/recursos/{$r->id_recurso}/movimientos", ['tipo' => 'SALIDA', 'cantidad' => 2, 'motivo' => 'Uso'])->assertStatus(409);
    }
}
