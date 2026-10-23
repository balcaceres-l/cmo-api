<?php

namespace Database\Seeders;

use App\Models\Consultorio;
use Illuminate\Database\Seeder;

class ConsultorioSeeder extends Seeder
{
    /** Planta 1: consultorios de consulta general. */
    public function run(): void
    {
        foreach (range(101, 104) as $n) {
            Consultorio::updateOrCreate(['codigo' => "C-$n"], ['planta' => 1, 'nombre' => "Consultorio $n · Consulta general", 'activo' => true]);
        }
    }
}
