<?php

namespace App\Http\Controllers\Api;

use App\Exceptions\ConflictoException;
use App\Models\TurnoPersonal;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

class TurnoController extends ApiController
{
    public function index(Request $r)
    {
        return $this->success($this->paginar(TurnoPersonal::with('usuario')->orderBy('fecha')->orderBy('hora_inicio'), $r));
    }

    /**
     * Devuelve siempre una lista: un turno nocturno (hora_fin menor que
     * hora_inicio, p. ej. 22:00–06:00) se guarda como dos registros, uno por
     * día, para que la agenda y "médicos de turno" trabajen dentro de cada fecha.
     */
    public function store(Request $r)
    {
        $d = $this->datos($r, nocturno: true);
        $partes = $d['hora_fin'] > $d['hora_inicio']
            ? [$d]
            : [
                ['hora_fin' => self::FIN_DEL_DIA] + $d,
                ['fecha' => Carbon::parse($d['fecha'])->addDay()->toDateString(), 'hora_inicio' => '00:00'] + $d,
            ];

        return DB::transaction(function () use ($partes) {
            foreach ($partes as $parte) {
                $this->sinSolape($parte);
            }

            return $this->success(array_map(fn ($p) => TurnoPersonal::create($p), $partes), count($partes) > 1 ? 'Turno nocturno creado en dos partes.' : 'Turno creado.', 201);
        });
    }

    private const FIN_DEL_DIA = '23:59:59';

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

    /** Al editar no se permite cruzar la medianoche: un turno nocturno se crea nuevo. */
    private function datos(Request $r, bool $nocturno = false): array
    {
        return $r->validate([
            'id_usuario' => ['required', 'uuid', $this->reglaPersonal()],
            'fecha' => 'required|date',
            'hora_inicio' => 'required|date_format:H:i',
            'hora_fin' => $nocturno ? 'required|date_format:H:i|different:hora_inicio' : 'required|date_format:H:i|after:hora_inicio',
            'area' => 'required|string|max:100',
        ]);
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
