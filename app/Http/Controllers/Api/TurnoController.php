<?php

namespace App\Http\Controllers\Api;

use App\Exceptions\ConflictoException;
use App\Models\TurnoPersonal;
use Illuminate\Http\Request;

class TurnoController extends ApiController
{
    public function index(Request $r)
    {
        return $this->success($this->paginar(TurnoPersonal::with('usuario')->orderBy('fecha')->orderBy('hora_inicio'), $r));
    }

    public function store(Request $r)
    {
        $d = $this->datos($r);
        $this->sinSolape($d);

        return $this->success(TurnoPersonal::create($d), 'Turno creado.', 201);
    }

    public function show(TurnoPersonal $turno)
    {
        return $this->success($turno->load('usuario'));
    }

    public function update(Request $r, TurnoPersonal $turno)
    {
        $d = $this->datos($r);
        $this->sinSolape($d, $turno->getKey());
        $turno->update($d);

        return $this->success($turno->refresh());
    }

    public function destroy(TurnoPersonal $turno)
    {
        $turno->delete();

        return $this->success(null, 'Turno eliminado.');
    }

    public function mios(Request $r)
    {
        return $this->success($this->paginar($r->user()->turnos()->orderBy('fecha')->getQuery(), $r));
    }

    private function datos(Request $r): array
    {
        return $r->validate(['id_usuario' => ['required', 'uuid', $this->reglaPersonal()], 'fecha' => 'required|date', 'hora_inicio' => 'required|date_format:H:i', 'hora_fin' => 'required|date_format:H:i|after:hora_inicio', 'area' => 'required|string|max:100']);
    }

    private function sinSolape(array $d, ?string $ignorar = null): void
    {
        $q = TurnoPersonal::where('id_usuario', $d['id_usuario'])->whereDate('fecha', $d['fecha'])->where('hora_inicio', '<', $d['hora_fin'])->where('hora_fin', '>', $d['hora_inicio']);
        if ($ignorar) {
            $q->where('id_turno', '!=', $ignorar);
        }if ($q->exists()) {
            throw new ConflictoException('El turno se solapa con otro turno de la misma persona.');
        }
    }
}
