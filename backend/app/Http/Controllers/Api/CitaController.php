<?php

namespace App\Http\Controllers\Api;

use App\Exceptions\ConflictoException;
use App\Models\Cita;
use App\Models\Paciente;
use App\Models\User;
use App\Services\AgendaService;
use Carbon\Carbon;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

class CitaController extends ApiController
{
    public function __construct(private AgendaService $agenda) {}

    public function index(Request $r)
    {
        // La consulta (solo su id y si está cerrada) permite a la app retomar una consulta abierta.
        $q = Cita::with(['paciente', 'medico.especialidad', 'especialidad', 'consultorio', 'consulta:id_consulta,id_cita,fecha_hora_fin'])
            ->orderByDesc('fecha')
            ->orderBy('hora_inicio');

        if ($r->user()->rol === 'MEDICO') {
            $q->where(function ($sub) use ($r) {
                $sub->where('id_medico', $r->user()->id);
                if ($r->user()->id_especialidad) {
                    $sub->orWhere(fn ($s) => $s->whereNull('id_medico')->where('id_especialidad', $r->user()->id_especialidad));
                }
            });
        }

        return $this->success($this->paginar($q, $r));
    }

    public function store(Request $r)
    {
        $d = $this->datos($r);
        if ($r->user()->rol === 'MEDICO') {
            $d['id_medico'] = $r->user()->id;
            $d['id_especialidad'] ??= $r->user()->id_especialidad;
            // El médico solo agenda seguimientos: si pudiera agendar a cualquier
            // paciente, la cita lo haría tratante y le abriría el expediente.
            abort_unless(
                Paciente::whereKey($d['id_paciente'])->tratadoPor($r->user()->id)->exists(),
                403,
                'Solo puedes agendar seguimientos de tus pacientes; la primera cita la agenda recepción.',
            );
        }

        return DB::transaction(function () use ($d, $r) {
            if (! empty($d['id_medico'])) {
                $d['id_especialidad'] ??= User::whereKey($d['id_medico'])->value('id_especialidad');
                $this->agenda->validar($d['id_medico'], $d['fecha'], $d['hora_inicio'], $d['hora_fin'], null, $d['id_consultorio'] ?? null, $d['id_paciente']);
            } else {
                $d['id_medico'] = null;
                $this->agenda->validarPorEspecialidad($d['id_especialidad'], $d['fecha'], $d['hora_inicio'], $d['hora_fin'], null, $d['id_consultorio'] ?? null, $d['id_paciente']);
            }
            $d['id_creado_por'] = $r->user()->id;
            $d['estado'] = 'AGENDADA';

            return $this->success(Cita::create($d), 'Cita agendada.', 201);
        });
    }

    public function disponibilidad(Request $r)
    {
        $d = $r->validate([
            'id_especialidad' => 'nullable|uuid|exists:especialidad,id_especialidad',
            'id_medico' => ['nullable', $this->reglaMedico()],
            'fecha' => 'required|date',
        ]);

        if (! empty($d['id_especialidad'])) {
            return $this->success($this->agenda->bloquesDisponiblesPorEspecialidad($d['id_especialidad'], $d['fecha']));
        }
        if (! empty($d['id_medico'])) {
            return $this->success($this->agenda->bloquesDisponibles($d['id_medico'], $d['fecha']));
        }

        throw ValidationException::withMessages(['id_especialidad' => 'Debes indicar una especialidad o un médico.']);
    }

    public function cancelar(Request $r, Cita $cita)
    {
        $d = $r->validate(['motivo_cancelacion' => 'required|string']);
        $cita->exigirEstado(['SOLICITADA', 'AGENDADA', 'EN_ESPERA'], 'cancelar');
        $cita->update($d + ['estado' => 'CANCELADA']);

        return $this->success($cita->refresh());
    }

    public function reprogramar(Request $r, Cita $cita)
    {
        $d = $this->datos($r, reprogramar: true);

        return DB::transaction(function () use ($d, $cita) {
            $cita = Cita::lockForUpdate()->findOrFail($cita->getKey());
            $cita->exigirEstado(['AGENDADA', 'EN_ESPERA'], 'reprogramar');
            // Sin consultorio nuevo, la cita conserva el que tenía.
            $consultorio = $d['id_consultorio'] ?? $cita->id_consultorio;
            if (! empty($d['id_medico'])) {
                $d['id_especialidad'] ??= User::whereKey($d['id_medico'])->value('id_especialidad');
                $this->agenda->validar($d['id_medico'], $d['fecha'], $d['hora_inicio'], $d['hora_fin'], $cita->getKey(), $consultorio, $cita->id_paciente);
            } else {
                $d['id_medico'] = null;
                $this->agenda->validarPorEspecialidad($d['id_especialidad'], $d['fecha'], $d['hora_inicio'], $d['hora_fin'], $cita->getKey(), $consultorio, $cita->id_paciente);
            }
            $cita->update($d + ['estado' => 'AGENDADA']);

            return $this->success($cita->refresh());
        });
    }

    public function registrarLlegada(Cita $cita)
    {
        $cita->exigirEstado(['AGENDADA'], 'registrar la llegada de');
        if (! $cita->fecha->isToday()) {
            throw new ConflictoException('La llegada solo se registra el día de la cita.');
        }
        $cita->update(['estado' => 'EN_ESPERA', 'hora_llegada' => now()]);

        return $this->success($cita->refresh());
    }

    public function solicitar(Request $r)
    {
        $d = $r->validate([
            'motivo' => 'required|string',
            'id_especialidad' => 'nullable|uuid|exists:especialidad,id_especialidad',
        ]);
        $p = $r->user()->paciente()->firstOrFail();

        return $this->success(Cita::create([
            'id_paciente' => $p->getKey(),
            'id_especialidad' => $d['id_especialidad'] ?? null,
            'motivo' => $d['motivo'],
            'estado' => 'SOLICITADA',
            'id_creado_por' => $r->user()->id,
        ]), 'Solicitud creada.', 201);
    }

    public function confirmar(Request $r, Cita $cita)
    {
        $d = $r->validate([
            'id_especialidad' => 'nullable|uuid|exists:especialidad,id_especialidad',
            'id_medico' => ['nullable', $this->reglaMedico()],
            'id_consultorio' => ['nullable', $this->reglaConsultorio()],
            'fecha' => 'required|date|after_or_equal:today',
            'hora_inicio' => 'required|date_format:H:i',
        ]);
        if (empty($d['id_especialidad']) && empty($d['id_medico'])) {
            throw ValidationException::withMessages(['id_especialidad' => 'Debes seleccionar una especialidad o un médico.']);
        }
        $d['hora_fin'] = $this->horaFin($d['hora_inicio']);

        return DB::transaction(function () use ($d, $cita) {
            $cita = Cita::lockForUpdate()->findOrFail($cita->getKey());
            $cita->exigirEstado(['SOLICITADA'], 'confirmar');
            if (! empty($d['id_medico'])) {
                $d['id_especialidad'] ??= User::whereKey($d['id_medico'])->value('id_especialidad');
                $this->agenda->validar($d['id_medico'], $d['fecha'], $d['hora_inicio'], $d['hora_fin'], $cita->getKey(), $d['id_consultorio'] ?? null, $cita->id_paciente);
            } else {
                $d['id_medico'] = null;
                $this->agenda->validarPorEspecialidad($d['id_especialidad'], $d['fecha'], $d['hora_inicio'], $d['hora_fin'], $cita->getKey(), $d['id_consultorio'] ?? null, $cita->id_paciente);
            }
            $cita->update($d + ['estado' => 'AGENDADA']);

            return $this->success($cita->refresh());
        });
    }

    public function mias(Request $r)
    {
        // Del médico, el paciente solo necesita saber quién lo atiende: nada de
        // usuario de acceso, correo ni teléfono del personal.
        $citas = $r->user()->paciente()->firstOrFail()->citas()
            ->with([
                'medico:id,nombre_completo,cargo,rol,id_especialidad',
                'medico.especialidad:id_especialidad,nombre',
                'especialidad:id_especialidad,nombre',
                'consultorio:id_consultorio,codigo,nombre,planta',
            ])
            ->orderByDesc('fecha');

        return $this->success($this->paginar($citas->getQuery(), $r));
    }

    private function datos(Request $r, bool $reprogramar = false): array
    {
        $d = $r->validate([
            // Reprogramar mueve la cita en el tiempo; no la reasigna a otro paciente.
            'id_paciente' => $reprogramar ? 'prohibited' : 'required|exists:paciente,id_paciente',
            'id_especialidad' => 'nullable|uuid|exists:especialidad,id_especialidad',
            'id_medico' => ['nullable', $this->reglaMedico()],
            'id_consultorio' => ['nullable', $this->reglaConsultorio()],
            'fecha' => 'required|date|after_or_equal:today',
            'hora_inicio' => 'required|date_format:H:i',
            'hora_fin' => 'nullable|date_format:H:i|after:hora_inicio',
            'motivo' => $reprogramar ? 'sometimes|string' : 'required|string',
        ]);

        if ($r->user()->rol !== 'MEDICO' && empty($d['id_especialidad']) && empty($d['id_medico'])) {
            throw ValidationException::withMessages([
                'id_especialidad' => 'Debes seleccionar una especialidad o un médico.',
            ]);
        }

        $d['hora_fin'] = $this->horaFin($d['hora_inicio'], $d['hora_fin'] ?? null);

        return $d;
    }

    /**
     * Fin de la cita: el indicado o el inicio más la duración configurada. Una
     * cita no puede cruzar la medianoche: con un fin menor que el inicio la
     * comparación de horarios dejaría de detectar solapes.
     */
    private function horaFin(string $inicio, ?string $fin = null): string
    {
        $fin ??= Carbon::createFromFormat('H:i', $inicio)->addMinutes((int) config('hospital.duracion_cita_min'))->format('H:i');
        if ($fin <= $inicio) {
            throw ValidationException::withMessages(['hora_inicio' => 'La cita debe terminar el mismo día; elige una hora más temprana.']);
        }

        return $fin;
    }
}
