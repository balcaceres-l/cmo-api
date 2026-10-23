<?php

namespace App\Http\Controllers\Api;

use App\Exceptions\ConflictoException;
use App\Models\Cita;
use App\Models\Consulta;
use App\Models\Receta;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class ConsultaController extends ApiController
{
    public function store(Request $r, Cita $cita)
    {
        abort_unless($cita->id_medico === $r->user()->id, 403);

        return DB::transaction(function () use ($cita, $r) {
            $cita = Cita::lockForUpdate()->findOrFail($cita->getKey());
            $cita->exigirEstado(['AGENDADA', 'EN_ESPERA'], 'iniciar la consulta de');
            try {
                $c = Consulta::create(['id_cita' => $cita->getKey(), 'id_medico' => $r->user()->id, 'fecha_hora_inicio' => now(), 'motivo' => $cita->motivo]);
            } catch (UniqueConstraintViolationException) {
                throw new ConflictoException('La cita ya tiene una consulta.');
            }
            $cita->update(['estado' => 'EN_ATENCION']);

            return $this->success($c, 'Consulta iniciada.', 201);
        });
    }

    public function show(Request $r, Consulta $consulta)
    {
        $this->authorize('view', $consulta);

        return $this->success($consulta->load(['cita.paciente', 'recetas', 'ordenesLaboratorio.examen']));
    }

    public function update(Request $r, Consulta $consulta)
    {
        $this->propiaAbierta($r, $consulta);
        $consulta->update($r->validate(['diagnostico' => 'nullable|string', 'motivo' => 'nullable|string', 'notas' => 'nullable|string']));

        return $this->success($consulta->refresh());
    }

    public function finalizar(Request $r, Consulta $consulta)
    {
        $this->propiaAbierta($r, $consulta);

        return DB::transaction(function () use ($consulta) {
            $consulta->update(['fecha_hora_fin' => now()]);
            $consulta->cita()->update(['estado' => 'ATENDIDA']);

            return $this->success($consulta->refresh(), 'Consulta finalizada.');
        });
    }

    public function receta(Request $r, Consulta $consulta)
    {
        $this->propiaAbierta($r, $consulta);
        $d = $r->validate(['medicamento' => 'required|string', 'dosis' => 'required|string', 'frecuencia' => 'required|string', 'duracion' => 'required|string', 'indicaciones' => 'nullable|string']);

        return $this->success($consulta->recetas()->create($d), 'Receta creada.', 201);
    }

    public function eliminarReceta(Request $r, Receta $receta)
    {
        $this->propiaAbierta($r, $receta->consulta);
        $receta->delete();

        return $this->success(null, 'Receta eliminada.');
    }

    private function propiaAbierta(Request $r, Consulta $c): void
    {
        abort_unless($c->id_medico === $r->user()->id, 403);
        abort_if($c->fecha_hora_fin, 409, 'La consulta ya está cerrada.');
    }
}
