<?php

namespace Database\Seeders;

use App\Models\TurnoPersonal;
use App\Models\User;
use Illuminate\Database\Seeder;

class TurnoSeeder extends Seeder
{
    public function run(): void
    {
        $inicio = now()->startOfWeek();
        foreach (User::where('rol', '!=', 'PACIENTE')->get() as $u) {
            foreach (range(0, 4) as $d) {
                TurnoPersonal::updateOrCreate(['id_usuario' => $u->id, 'fecha' => $inicio->copy()->addDays($d)->toDateString(), 'hora_inicio' => '08:00:00'], ['hora_fin' => '16:00:00', 'area' => $u->cargo ?? 'Hospital']);
            }
        }
    }
}
