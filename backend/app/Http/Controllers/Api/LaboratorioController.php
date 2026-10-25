<?php

namespace App\Http\Controllers\Api;

use App\Exceptions\ConflictoException;
use App\Models\Consulta;
use App\Models\ExamenCatalogo;
use App\Models\OrdenLaboratorio;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

/**
 * El laboratorio solo informa estados: PENDIENTE → EN_PROCESO → COMPLETADO.
 * El médico puede cancelar (CANCELADA) una orden que sigue PENDIENTE.
 * Los datos del examen no pasan por el sistema.
 */
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

        return $this->success($this->paginar($q->orderByDesc('fecha_solicitud'), $r));
    }

    public function iniciar(OrdenLaboratorio $orden)
    {
        return $this->avanzar($orden, 'PENDIENTE', ['estado' => 'EN_PROCESO', 'fecha_inicio' => now()], 'Orden en proceso.');
    }

    public function completar(Request $r, OrdenLaboratorio $orden)
    {
        return $this->avanzar($orden, 'EN_PROCESO', ['estado' => 'COMPLETADO', 'fecha_resultado' => now(), 'id_registrado_por' => $r->user()->id], 'Orden completada.');
    }

    /** El médico que la pidió puede cancelarla mientras el laboratorio no la haya iniciado. */
    public function cancelar(Request $r, OrdenLaboratorio $orden)
    {
        abort_unless($orden->id_medico === $r->user()->id, 403);
        $d = $r->validate(['motivo_cancelacion' => 'nullable|string']);

        return DB::transaction(function () use ($orden, $d) {
            $orden = OrdenLaboratorio::lockForUpdate()->findOrFail($orden->getKey());
            if ($orden->estado !== 'PENDIENTE') {
                throw new ConflictoException("La orden solo puede cancelarse mientras está PENDIENTE; estado actual: {$orden->estado}.");
            }
            $orden->update($d + ['estado' => 'CANCELADA', 'fecha_cancelacion' => now()]);

            return $this->success($orden->refresh(), 'Orden cancelada.');
        });
    }

    public function mias(Request $r)
    {
        $p = $r->user()->paciente()->firstOrFail();

        return $this->success($this->paginar($p->ordenesLaboratorio()->with('examen')->orderByDesc('fecha_solicitud')->getQuery(), $r));
    }

    /** Mueve la orden un paso, solo hacia adelante y con la fila bloqueada. */
    private function avanzar(OrdenLaboratorio $orden, string $desde, array $cambios, string $mensaje)
    {
        return DB::transaction(function () use ($orden, $desde, $cambios, $mensaje) {
            $orden = OrdenLaboratorio::lockForUpdate()->findOrFail($orden->getKey());
            if ($orden->estado !== $desde) {
                throw new ConflictoException("La orden solo puede avanzar desde {$desde}; estado actual: {$orden->estado}.");
            }
            $orden->update($cambios);

            return $this->success($orden->refresh(), $mensaje);
        });
    }
}
