<?php

namespace Database\Seeders;

use App\Models\Aviso;
use App\Models\User;
use Illuminate\Database\Seeder;

class AvisoSeeder extends Seeder
{
    public function run(): void
    {
        $a = User::where('rol', 'ADMINISTRADOR')->first();
        if (! $a) {
            return;
        }foreach ([['Horario de visitas', 'Las visitas son de 14:00 a 17:00.', true], ['Campaña de vacunación', 'Consulta fechas en recepción.', true], ['Reunión de personal', 'Reunión interna semanal.', false]] as [$t,$c,$p]) {
            Aviso::updateOrCreate(['titulo' => $t], ['id_autor' => $a->id, 'contenido' => $c, 'publico' => $p, 'vigente_desde' => now()->subDay(), 'vigente_hasta' => now()->addMonth()]);
        }
    }
}
