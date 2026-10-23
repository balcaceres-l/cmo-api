<?php

namespace Database\Seeders;

use App\Models\Ambulancia;
use Illuminate\Database\Seeder;

class AmbulanciaSeeder extends Seeder
{
    public function run(): void
    {
        Ambulancia::updateOrCreate(['placa' => 'P-123456'], ['estado' => 'DISPONIBLE']);
    }
}
