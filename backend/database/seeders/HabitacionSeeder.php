<?php

namespace Database\Seeders;

use App\Models\Cama;
use App\Models\Habitacion;
use Illuminate\Database\Seeder;

class HabitacionSeeder extends Seeder
{
    public function run(): void
    {
        foreach (range(301, 305) as $n) {
            $h = Habitacion::updateOrCreate(['codigo' => "VIP-$n"], ['planta' => 3, 'tipo' => 'VIP', 'capacidad' => 1]);
            Cama::updateOrCreate(['codigo' => "VIP-$n"], ['id_habitacion' => $h->getKey(), 'estado' => 'DISPONIBLE']);
        }foreach (range(201, 206) as $n) {
            $h = Habitacion::updateOrCreate(['codigo' => "HAB-$n"], ['planta' => 2, 'tipo' => 'SEMIPRIVADA', 'capacidad' => 2]);
            foreach (['A', 'B'] as $l) {
                Cama::updateOrCreate(['codigo' => "HAB-$n-$l"], ['id_habitacion' => $h->getKey(), 'estado' => 'DISPONIBLE']);
            }
        }
    }
}
