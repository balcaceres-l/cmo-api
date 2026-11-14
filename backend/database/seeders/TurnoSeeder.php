<?php

namespace Database\Seeders;

use App\Models\TurnoPersonal;
use App\Models\User;
use Illuminate\Database\Seeder;

class TurnoSeeder extends Seeder
{
    private function crearTurnoNoche(User $u, string $fecha, string $area): void
    {
        TurnoPersonal::updateOrCreate(
            ['id_usuario' => $u->id, 'fecha' => $fecha, 'hora_inicio' => '00:00:00'],
            ['hora_fin' => '08:00:00', 'area' => $area]
        );
    }

    private function crearTurnoManana(User $u, string $fecha, string $area): void
    {
        TurnoPersonal::updateOrCreate(
            ['id_usuario' => $u->id, 'fecha' => $fecha, 'hora_inicio' => '08:00:00'],
            ['hora_fin' => '16:00:00', 'area' => $area]
        );
    }

    private function crearTurnoTarde(User $u, string $fecha, string $area): void
    {
        TurnoPersonal::updateOrCreate(
            ['id_usuario' => $u->id, 'fecha' => $fecha, 'hora_inicio' => '16:00:00'],
            ['hora_fin' => '23:59:59', 'area' => $area]
        );
    }

    /**
     * Siembra únicamente historial de la semana pasada (Lunes a Sábado) en bloques de 8h
     * iniciando la jornada a las 08:00 (00-08, 08-16, 16-24), dejando la semana actual 100% vacía.
     */
    public function run(): void
    {
        $semanaPasada = now()->subWeek()->startOfWeek();

        $emergencias = User::where('rol', 'MEDICO')
            ->where(fn ($q) => $q->where('cargo', 'Emergencia')->orWhereHas('especialidad', fn ($e) => $e->where('nombre', 'Emergencia')))
            ->orderBy('usuario')
            ->get()
            ->values();
        $enfermeras = User::where('rol', 'RECEPCIONISTA')->where('cargo', 'Enfermera')->orderBy('usuario')->get()->values();
        $recepcionistas = User::where('rol', 'RECEPCIONISTA')->where('cargo', 'Recepcionista')->orderBy('usuario')->get()->values();

        // Cubrir Lunes a Sábado de la semana pasada (0..5) en turnos de 8h (00-08, 08-16, 16-23:59)
        foreach (range(0, 5) as $offset) {
            $fecha = $semanaPasada->copy()->addDays($offset)->toDateString();

            foreach ([
                [$emergencias, 'Servicio de Emergencias'],
                [$enfermeras, 'Triaje y Enfermería'],
                [$recepcionistas, 'Admisión y Recepción'],
            ] as [$equipo, $area]) {
                $total = $equipo->count();
                if ($total < 3) {
                    continue;
                }
                $this->crearTurnoNoche($equipo[($offset * 3) % $total], $fecha, $area);
                $this->crearTurnoManana($equipo[($offset * 3 + 1) % $total], $fecha, $area);
                $this->crearTurnoTarde($equipo[($offset * 3 + 2) % $total], $fecha, $area);
            }
        }

        // Especialidades de la semana pasada (08:00-16:00)
        $especialistas = [
            'medico.general' => [0, 2, 4],
            'medico.general2' => [1, 3, 5],
            'medico.alergista' => [1, 4],
            'medico.pediatria' => [0, 3],
            'medico.cardiologia' => [2],
            'medico.ginecologia' => [3],
        ];

        foreach ($especialistas as $usuario => $dias) {
            $u = User::where('usuario', $usuario)->first();
            if (!$u) {
                continue;
            }
            foreach ($dias as $offset) {
                $this->crearTurnoManana($u, $semanaPasada->copy()->addDays($offset)->toDateString(), 'Consulta Externa');
            }
        }
    }
}
