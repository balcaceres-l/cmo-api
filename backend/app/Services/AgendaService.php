<?php

namespace App\Services;

use App\Exceptions\ConflictoException;
use App\Models\Cita;
use App\Models\TurnoPersonal;
use App\Models\User;
use Carbon\Carbon;
use Illuminate\Support\Collection;
use Illuminate\Validation\ValidationException;

class AgendaService
{
    public function bloquesDisponibles(string $medico, string $fecha): array
    {
        $u = User::find($medico);
        if ($u && ! $u->estaVigenteEn($fecha)) {
            return [];
        }
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
                // Hoy no se ofrecen bloques que ya empezaron.
                if (! $solapa && $cursor->gt(now())) {
                    $bloques[] = ['hora_inicio' => $cursor->format('H:i'), 'hora_fin' => $hasta->format('H:i')];
                }
                $cursor = $hasta;
            }
        }

        return $bloques;
    }

    public function bloquesDisponiblesPorEspecialidad(string $idEspecialidad, string $fecha): array
    {
        $medicoIds = $this->medicosVigentesDeEspecialidad($idEspecialidad, $fecha);
        if ($medicoIds->isEmpty()) {
            return [];
        }

        $dur = (int) config('hospital.duracion_cita_min', 30);
        $turnos = TurnoPersonal::whereIn('id_usuario', $medicoIds)
            ->whereDate('fecha', $fecha)
            ->orderBy('hora_inicio')
            ->get();

        if ($turnos->isEmpty()) {
            return [];
        }

        $ocupados = Cita::whereDate('fecha', $fecha)
            ->whereNotIn('estado', ['CANCELADA', 'NO_ASISTIO', 'SOLICITADA'])
            ->where(fn ($q) => $q
                ->where('id_especialidad', $idEspecialidad)
                ->orWhereIn('id_medico', $medicoIds))
            ->get();

        $slots = [];
        foreach ($turnos as $turno) {
            $cursor = Carbon::parse("$fecha $turno->hora_inicio");
            $fin = Carbon::parse("$fecha $turno->hora_fin");
            while ($cursor->copy()->addMinutes($dur)->lte($fin)) {
                $hasta = $cursor->copy()->addMinutes($dur);
                if ($cursor->gt(now())) {
                    $key = $cursor->format('H:i').'|'.$hasta->format('H:i');
                    $slots[$key] = [$cursor->copy(), $hasta->copy()];
                }
                $cursor = $hasta;
            }
        }

        ksort($slots);
        $bloques = [];
        foreach ($slots as [$inicio, $fin]) {
            $capacidad = $turnos
                ->filter(fn ($t) => Carbon::parse("$fecha $t->hora_inicio")->lte($inicio) && Carbon::parse("$fecha $t->hora_fin")->gte($fin))
                ->pluck('id_usuario')
                ->unique()
                ->count();

            $ocupadas = $ocupados
                ->filter(fn ($c) => Carbon::parse("$fecha $c->hora_inicio")->lt($fin) && Carbon::parse("$fecha $c->hora_fin")->gt($inicio))
                ->count();

            if ($ocupadas < $capacidad) {
                $bloques[] = ['hora_inicio' => $inicio->format('H:i'), 'hora_fin' => $fin->format('H:i')];
            }
        }

        return $bloques;
    }

    /**
     * La cita debe caber en un turno del médico y no chocar con otra cita
     * activa del mismo médico, del mismo paciente ni, si se indica, del mismo consultorio.
     */
    public function validar(string $medico, string $fecha, string $inicio, string $fin, ?string $ignorar = null, ?string $consultorio = null, ?string $paciente = null): void
    {
        if (Carbon::parse("$fecha $inicio")->lte(now())) {
            throw ValidationException::withMessages(['hora_inicio' => 'La hora de la cita ya pasó; elige una hora posterior.']);
        }

        $u = User::find($medico);
        if ($u && ! $u->estaVigenteEn($fecha)) {
            throw ValidationException::withMessages(['fecha' => 'El médico seleccionado no se encuentra vigente/contratado en la fecha de la cita.']);
        }

        $enTurno = TurnoPersonal::where('id_usuario', $medico)->whereDate('fecha', $fecha)->where('hora_inicio', '<=', $inicio)->where('hora_fin', '>=', $fin)->exists();
        if (! $enTurno) {
            throw ValidationException::withMessages(['hora_inicio' => 'La cita debe estar dentro de un turno del médico.']);
        }

        $choca = fn (string $columna, string $valor) => Cita::where($columna, $valor)
            ->whereDate('fecha', $fecha)
            ->whereNotIn('estado', ['CANCELADA', 'NO_ASISTIO', 'SOLICITADA'])
            ->where('hora_inicio', '<', $fin)
            ->where('hora_fin', '>', $inicio)
            ->when($ignorar, fn ($q) => $q->where('id_cita', '!=', $ignorar))
            ->lockForUpdate()
            ->exists();

        if ($choca('id_medico', $medico)) {
            throw new ConflictoException('El horario solicitado ya está ocupado.');
        }
        if ($consultorio && $choca('id_consultorio', $consultorio)) {
            throw new ConflictoException('El consultorio ya está ocupado a esa hora.');
        }
        if ($paciente && $choca('id_paciente', $paciente)) {
            throw new ConflictoException('El paciente ya tiene una cita a esa hora.');
        }
    }

    public function validarPorEspecialidad(string $idEspecialidad, string $fecha, string $inicio, string $fin, ?string $ignorar = null, ?string $consultorio = null, ?string $paciente = null): void
    {
        if (Carbon::parse("$fecha $inicio")->lte(now())) {
            throw ValidationException::withMessages(['hora_inicio' => 'La hora de la cita ya pasó; elige una hora posterior.']);
        }

        $medicoIds = $this->medicosVigentesDeEspecialidad($idEspecialidad, $fecha);
        if ($medicoIds->isEmpty()) {
            throw ValidationException::withMessages(['hora_inicio' => 'No hay médicos vigentes para la especialidad en esa fecha.']);
        }

        $enTurnoIds = TurnoPersonal::whereIn('id_usuario', $medicoIds)
            ->whereDate('fecha', $fecha)
            ->where('hora_inicio', '<=', $inicio)
            ->where('hora_fin', '>=', $fin)
            ->pluck('id_usuario')
            ->unique();

        $capacidad = $enTurnoIds->count();
        if ($capacidad === 0) {
            throw ValidationException::withMessages(['hora_inicio' => 'La cita debe estar dentro de un turno disponible de la especialidad seleccionada.']);
        }

        $ocupadas = Cita::whereDate('fecha', $fecha)
            ->whereNotIn('estado', ['CANCELADA', 'NO_ASISTIO', 'SOLICITADA'])
            ->where('hora_inicio', '<', $fin)
            ->where('hora_fin', '>', $inicio)
            ->where(fn ($q) => $q
                ->where('id_especialidad', $idEspecialidad)
                ->orWhereIn('id_medico', $enTurnoIds))
            ->when($ignorar, fn ($q) => $q->where('id_cita', '!=', $ignorar))
            ->lockForUpdate()
            ->count();

        if ($ocupadas >= $capacidad) {
            throw new ConflictoException('El horario solicitado ya está ocupado.');
        }

        $choca = fn (string $columna, string $valor) => Cita::where($columna, $valor)
            ->whereDate('fecha', $fecha)
            ->whereNotIn('estado', ['CANCELADA', 'NO_ASISTIO', 'SOLICITADA'])
            ->where('hora_inicio', '<', $fin)
            ->where('hora_fin', '>', $inicio)
            ->when($ignorar, fn ($q) => $q->where('id_cita', '!=', $ignorar))
            ->lockForUpdate()
            ->exists();

        if ($consultorio && $choca('id_consultorio', $consultorio)) {
            throw new ConflictoException('El consultorio ya está ocupado a esa hora.');
        }
        if ($paciente && $choca('id_paciente', $paciente)) {
            throw new ConflictoException('El paciente ya tiene una cita a esa hora.');
        }
    }

    private function medicosVigentesDeEspecialidad(string $idEspecialidad, string $fecha): Collection
    {
        return User::where('rol', 'MEDICO')
            ->where('estado', 'ACTIVO')
            ->where('id_especialidad', $idEspecialidad)
            ->get()
            ->filter(fn (User $u) => $u->estaVigenteEn($fecha))
            ->pluck('id')
            ->values();
    }
}
