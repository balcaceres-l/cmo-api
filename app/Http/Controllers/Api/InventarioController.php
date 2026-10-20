<?php

namespace App\Http\Controllers\Api;

use App\Exceptions\ConflictoException;
use App\Models\Recurso;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class InventarioController extends ApiController
{
    public function index(Request $r)
    {
        return $this->success($this->paginar(Recurso::orderBy('nombre'), $r));
    }

    public function store(Request $r)
    {
        return $this->success(Recurso::create($this->datos($r)), 'Recurso creado.', 201);
    }

    public function show(Recurso $recurso)
    {
        return $this->success($recurso);
    }

    public function update(Request $r, Recurso $recurso)
    {
        $recurso->update($this->datos($r, true));

        return $this->success($recurso->refresh());
    }

    public function destroy(Recurso $recurso)
    {
        $recurso->delete();

        return $this->success(null, 'Recurso eliminado.');
    }

    public function movimiento(Request $r, Recurso $recurso)
    {
        $d = $r->validate(['tipo' => 'required|in:ENTRADA,SALIDA', 'cantidad' => 'required|integer|min:1', 'motivo' => 'required|string']);
        if ($r->user()->rol === 'RECEPCIONISTA' && $d['tipo'] !== 'SALIDA') {
            abort(403);
        }

return DB::transaction(function () use ($d, $r, $recurso) {
            $recurso = Recurso::lockForUpdate()->findOrFail($recurso->getKey());
            $n = $recurso->cantidad + ($d['tipo'] === 'ENTRADA' ? $d['cantidad'] : -$d['cantidad']);
            if ($n < 0) {
                throw new ConflictoException('La salida dejaría el inventario con cantidad negativa.');
            }$recurso->update(['cantidad' => $n]);
            $m = $recurso->movimientos()->create($d + ['id_usuario' => $r->user()->id, 'fecha' => now()]);

            return $this->success($m, 'Movimiento registrado.', 201);
        });
    }

    private function datos(Request $r, bool $p = false): array
    {
        return $r->validate(['nombre' => ($p ? 'sometimes' : 'required').'|string', 'categoria' => ($p ? 'sometimes' : 'required').'|in:Medicamento,Insumo,Equipo', 'unidad' => ($p ? 'sometimes' : 'required').'|string', 'cantidad' => 'sometimes|integer|min:0', 'stock_minimo' => ($p ? 'sometimes' : 'required').'|integer|min:0', 'ubicacion' => 'nullable|string']);
    }
}
