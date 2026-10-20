<?php

namespace Database\Seeders;

use App\Models\Recurso;
use Illuminate\Database\Seeder;

class RecursoSeeder extends Seeder
{
    public function run(): void
    {
        $items = [['Paracetamol', 'Medicamento', 'tabletas', 200, 50], ['Amoxicilina', 'Medicamento', 'cápsulas', 20, 30], ['Jeringas', 'Insumo', 'unidades', 300, 100], ['Guantes', 'Insumo', 'cajas', 15, 20], ['Gasas', 'Insumo', 'paquetes', 100, 30], ['Oxímetro', 'Equipo', 'unidades', 8, 2], ['Silla de ruedas', 'Equipo', 'unidades', 6, 2], ['Suero fisiológico', 'Medicamento', 'bolsas', 80, 25]];
        foreach ($items as [$n,$c,$u,$q,$m]) {
            Recurso::updateOrCreate(['nombre' => $n], ['categoria' => $c, 'unidad' => $u, 'cantidad' => $q, 'stock_minimo' => $m, 'ubicacion' => 'Bodega central']);
        }
    }
}
