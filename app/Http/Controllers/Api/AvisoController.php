<?php

namespace App\Http\Controllers\Api;

use App\Models\Aviso;
use Illuminate\Http\Request;

class AvisoController extends ApiController
{
    public function index(Request $r)
    {
        return $this->success($this->paginar(Aviso::orderByDesc('vigente_desde'), $r));
    }

    public function store(Request $r)
    {
        $d = $this->datos($r);
        $d['id_autor'] = $r->user()->id;

        return $this->success(Aviso::create($d), 'Aviso creado.', 201);
    }

    public function show(Aviso $aviso)
    {
        return $this->success($aviso);
    }

    public function update(Request $r, Aviso $aviso)
    {
        $aviso->update($this->datos($r, true));

        return $this->success($aviso->refresh());
    }

    public function destroy(Aviso $aviso)
    {
        $aviso->delete();

        return $this->success(null, 'Aviso eliminado.');
    }

    private function datos(Request $r, bool $p = false): array
    {
        return $r->validate(['titulo' => ($p ? 'sometimes' : 'required').'|string', 'contenido' => ($p ? 'sometimes' : 'required').'|string', 'publico' => ($p ? 'sometimes' : 'required').'|boolean', 'vigente_desde' => ($p ? 'sometimes' : 'required').'|date', 'vigente_hasta' => 'nullable|date|after:vigente_desde']);
    }
}
