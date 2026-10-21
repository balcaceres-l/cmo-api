<?php

namespace App\Http\Controllers\Api;

use App\Exceptions\ConflictoException;
use App\Models\Consulta;
use App\Models\ExamenCatalogo;
use App\Models\OrdenLaboratorio;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class LaboratorioController extends ApiController
{
    public function examenes(Request $r)
    {
        return $this->success($this->paginar(ExamenCatalogo::where('activo', true)->orderBy('nombre'), $r));
    }

    public function crear(Request $r, Consulta $consulta)
    {
        abort_unless($consulta->id_medico === $r->user()->id, 403);
        abort_if($consulta->fecha_hora_fin, 409, 'La consulta ya está cerrada.');
        $d = $r->validate(['id_examen' => 'required|exists:examen_catalogo,id_examen', 'observaciones' => 'nullable|string']);
        $o = $consulta->ordenesLaboratorio()->create($d + ['id_paciente' => $consulta->cita->id_paciente, 'id_medico' => $r->user()->id, 'estado' => 'PENDIENTE', 'fecha_solicitud' => now()]);

        return $this->success($o, 'Orden creada.', 201);
    }

    public function index(Request $r)
    {
        $q = OrdenLaboratorio::with(['paciente', 'examen'])->when($r->estado, fn ($q, $v) => $q->where('estado', $v))->when($r->paciente, fn ($q, $v) => $q->where('id_paciente', $v));
        if ($r->user()->rol === 'MEDICO') {
            $q->where('id_medico', $r->user()->id);
        }
        $data = $this->paginar($q->orderByDesc('fecha_solicitud'), $r);
        $data->getCollection()->each(fn ($o) => $this->protegerResultado($r, $o));

        return $this->success($data);
    }

    /**
     * Recepción procesa la cola y registra el resultado, pero el resultado es
     * dato clínico: solo lo ven el médico tratante, el paciente y el administrador.
     */
    private function protegerResultado(Request $r, OrdenLaboratorio $orden): OrdenLaboratorio
    {
        return $r->user()->rol === 'RECEPCIONISTA' ? $orden->makeHidden('resultado') : $orden;
    }

    public function iniciar(Request $r, OrdenLaboratorio $orden)
    {
        return DB::transaction(function () use ($r, $orden) {
            $orden = OrdenLaboratorio::lockForUpdate()->findOrFail($orden->getKey());
            if ($orden->estado !== 'PENDIENTE') {
                throw new ConflictoException('La orden solo puede iniciarse desde PENDIENTE.');
            }
            $orden->update(['estado' => 'EN_PROCESO', 'fecha_inicio' => now()]);

            return $this->success($this->protegerResultado($r, $orden->refresh()));
        });
    }

    public function completar(Request $r, OrdenLaboratorio $orden)
    {
        $d = $r->validate(['resultado' => 'required|string']);

        return DB::transaction(function () use ($r, $d, $orden) {
            $orden = OrdenLaboratorio::lockForUpdate()->findOrFail($orden->getKey());
            if ($orden->estado !== 'EN_PROCESO') {
                throw new ConflictoException('La orden solo puede completarse desde EN_PROCESO.');
            }
            $orden->update($d + ['estado' => 'COMPLETADO', 'fecha_resultado' => now(), 'id_registrado_por' => $r->user()->id]);

            return $this->success($this->protegerResultado($r, $orden->refresh()), 'Resultado registrado.');
        });
    }

    public function mias(Request $r)
    {
        $p = $r->user()->paciente()->firstOrFail();
        $data = $this->paginar($p->ordenesLaboratorio()->with('examen')->orderByDesc('fecha_solicitud')->getQuery(), $r);
        $data->getCollection()->each(fn ($o) => $o->estado === 'COMPLETADO' ? null : $o->setHidden(['resultado']));

        return $this->success($data);
    }
}
