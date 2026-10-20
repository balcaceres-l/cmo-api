<?php

namespace App\Services;

use App\Exceptions\ConflictoException;
use App\Models\Cita;
use App\Models\TurnoPersonal;
use Carbon\Carbon;
use Illuminate\Validation\ValidationException;

class AgendaService
{
    public function bloquesDisponibles(string $medico, string $fecha): array
    {
        $dur = (int) config('hospital.duracion_cita_min', 30);
        $turnos = TurnoPersonal::where('id_usuario', $medico)->whereDate('fecha', $fecha)->orderBy('hora_inicio')->get();
        $ocupados = Cita::where('id_medico', $medico)->whereDate('fecha', $fecha)->whereNotIn('estado', ['CANCELADA', 'NO_ASISTIO', 'SOLICITADA'])->get();
        $bloques = [];
        foreach ($turnos as $turno) {
            $cursor = Carbon::parse("$fecha $turno->hora_inicio");
            $fin = Carbon::parse("$fecha $turno->hora_fin");
            while ($cursor->copy()->addMinutes($dur)->lte($fin)) {
                $hasta = $cursor->copy()->addMinutes($dur);
                $solapa = $ocupados->contains(fn ($c) => Carbon::parse("$fecha $c->hora_inicio")->lt($hasta) && Carbon::parse("$fecha $c->hora_fin")->gt($cursor));
                if (! $solapa) {
                    $bloques[] = ['hora_inicio' => $cursor->format('H:i'), 'hora_fin' => $hasta->format('H:i')];
                }$cursor = $hasta;
            }
        }

return $bloques;
    }

    public function validar(string $medico, string $fecha, string $inicio, string $fin, ?string $ignorar = null): void
    {
        $enTurno = TurnoPersonal::where('id_usuario', $medico)->whereDate('fecha', $fecha)->where('hora_inicio', '<=', $inicio)->where('hora_fin', '>=', $fin)->exists();
        if (! $enTurno) {
            throw ValidationException::withMessages(['hora_inicio' => 'La cita debe estar dentro de un turno del médico.']);
        }$q = Cita::where('id_medico', $medico)->whereDate('fecha', $fecha)->whereNotIn('estado', ['CANCELADA', 'NO_ASISTIO', 'SOLICITADA'])->where('hora_inicio', '<', $fin)->where('hora_fin', '>', $inicio);
        if ($ignorar) {
            $q->where('id_cita', '!=', $ignorar);
        }if ($q->lockForUpdate()->exists()) {
            throw new ConflictoException('El horario solicitado ya está ocupado.');
        }
    }
}
