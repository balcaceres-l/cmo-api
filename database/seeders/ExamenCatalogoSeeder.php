<?php

namespace Database\Seeders;

use App\Models\ExamenCatalogo;
use Illuminate\Database\Seeder;

class ExamenCatalogoSeeder extends Seeder
{
    public function run(): void
    {
        foreach (['Hemograma completo', 'Glucosa en ayunas', 'Perfil lipídico', 'Examen general de orina', 'Creatinina', 'Prueba de embarazo', 'Radiografía de tórax'] as $n) {
            ExamenCatalogo::updateOrCreate(['nombre' => $n], ['activo' => true]);
        }
    }
}
