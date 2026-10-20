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
        $a = Ambulancia::with(['servicios' => fn ($q) => $q->where('estado', 'EN_CURSO')])->firstOrFail();
        if ($r->user()->rol !== 'MEDICO') {
            return $this->success($a);
        }

        // El médico no tiene acceso a la bitácora: ve el estado y la prioridad
        // del servicio en curso, igual que el evento en tiempo real, sin datos
        // de la persona atendida.
        $s = $a->servicios->first();

        return $this->success([
            'estado' => $a->estado,
            'servicio_activo' => $s ? ['prioridad' => $s->prioridad, 'hora_salida' => $s->hora_salida] : null,
        ]);
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
