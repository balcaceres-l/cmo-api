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

    public function test_cantidad_solo_cambia_con_movimientos_y_no_se_borra_con_historial(): void
    {
        Sanctum::actingAs(User::factory()->create(['rol' => 'ADMINISTRADOR']));
        $r = Recurso::create(['nombre' => 'Guantes', 'categoria' => 'Insumo', 'unidad' => 'caja', 'cantidad' => 5, 'stock_minimo' => 2]);

        $this->putJson("/api/v1/recursos/{$r->id_recurso}", ['cantidad' => 50])->assertStatus(422);
        $this->postJson("/api/v1/recursos/{$r->id_recurso}/movimientos", ['tipo' => 'ENTRADA', 'cantidad' => 3, 'motivo' => 'Compra'])->assertCreated();
        $this->assertSame(8, $r->refresh()->cantidad);
        $this->deleteJson("/api/v1/recursos/{$r->id_recurso}")->assertStatus(409);

        $sinHistorial = Recurso::create(['nombre' => 'Mascarillas', 'categoria' => 'Insumo', 'unidad' => 'caja', 'cantidad' => 0, 'stock_minimo' => 1]);
        $this->deleteJson("/api/v1/recursos/{$sinHistorial->id_recurso}")->assertOk();
    }
}
