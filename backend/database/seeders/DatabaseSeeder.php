<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $this->call([HabitacionSeeder::class, AmbulanciaSeeder::class, ExamenCatalogoSeeder::class, UsuarioDemoSeeder::class, TurnoSeeder::class, RecursoSeeder::class, AvisoSeeder::class]);
    }
}
