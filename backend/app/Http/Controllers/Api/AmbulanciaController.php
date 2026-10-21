<?php

namespace App\Http\Controllers\Api;

use App\Models\Ambulancia;
use App\Models\ServicioAmbulancia;
use App\Services\AmbulanciaService;
use Illuminate\Http\Request;

class AmbulanciaController extends ApiController
{
    public function __construct(private AmbulanciaService $servicio) {}

    public function show(Request $r)
    {
        $a = Ambulancia::with(['servicios' => fn ($q) => $q->where('estado', 'EN_CURSO'), 'servicios.ultimoReporte'])->firstOrFail();
        if ($r->user()->rol !== 'MEDICO') {
            return $this->success($a);
        }

        // El médico no tiene acceso a la bitácora: ve el estado, la prioridad
        // y el último reporte clínico del traslado (para preparar la atención),
        // sin nombre ni dirección de la persona atendida.
        $s = $a->servicios->first();
        $reporte = $s?->ultimoReporte;

        return $this->success([
            'estado' => $a->estado,
            'servicio_activo' => $s ? [
                'id_servicio' => $s->id_servicio,
                'prioridad' => $s->prioridad,
                'hora_salida' => $s->hora_salida,
                'ultimo_reporte' => $reporte?->only(['estado_paciente', 'observaciones', 'llegada_estimada', 'fecha']),
            ] : null,
        ]);
    }

    /**
     * Reporte desde la ambulancia en camino. `minutos_llegada` es lo que el
     * personal puede estimar en ruta; se guarda como hora estimada de llegada.
     */
    public function reportar(Request $r, ServicioAmbulancia $servicio)
    {
        $d = $r->validate([
            'estado_paciente' => 'required|in:ESTABLE,DELICADO,CRITICO',
            'observaciones' => 'nullable|string|max:2000',
            'minutos_llegada' => 'nullable|integer|min:0|max:240',
        ]);
        $datos = [
            'estado_paciente' => $d['estado_paciente'],
            'observaciones' => $d['observaciones'] ?? null,
            'llegada_estimada' => isset($d['minutos_llegada']) ? now()->addMinutes($d['minutos_llegada']) : null,
        ];

        return $this->success($this->servicio->reportar($servicio, $r->user(), $datos), 'Reporte enviado a emergencia.', 201);
    }

    public function reportes(ServicioAmbulancia $servicio)
    {
        return $this->success($servicio->reportes()->with('autor:id,nombre_completo,cargo')->orderBy('fecha')->get());
    }

    public function estado(Request $r)
    {
        $d = $r->validate(['estado' => 'required|in:DISPONIBLE,EN_MANTENIMIENTO']);

        return $this->success($this->servicio->cambiarEstado($d['estado']));
    }

    public function despachar(Request $r)
    {
        $d = $r->validate(['id_paciente' => 'nullable|exists:paciente,id_paciente|required_without:paciente_nombre', 'paciente_nombre' => 'nullable|string|required_without:id_paciente', 'direccion' => 'required|string', 'descripcion' => 'required|string', 'prioridad' => 'required|in:BAJA,MEDIA,ALTA,CRITICA']);
        $d['id_solicitante'] = $r->user()->id;

        return $this->success($this->servicio->despachar($d), 'Ambulancia despachada.', 201);
    }

    public function finalizar(Request $r, ServicioAmbulancia $servicio)
    {
        $d = $r->validate(['notas' => 'nullable|string']);

        return $this->success($this->servicio->finalizar($servicio, $d['notas'] ?? null));
    }

    public function bitacora(Request $r)
    {
        $q = ServicioAmbulancia::query()->when($r->desde, fn ($q, $v) => $q->whereDate('hora_solicitud', '>=', $v))->when($r->hasta, fn ($q, $v) => $q->whereDate('hora_solicitud', '<=', $v))->when($r->prioridad, fn ($q, $v) => $q->where('prioridad', $v))->when($r->estado, fn ($q, $v) => $q->where('estado', $v))->orderByDesc('hora_solicitud');

        return $this->success($this->paginar($q, $r));
    }
}
