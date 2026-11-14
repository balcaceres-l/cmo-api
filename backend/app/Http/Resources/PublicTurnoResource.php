<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class PublicTurnoResource extends JsonResource
{
    public function toArray(Request $r): array
    {
        return [
            'nombre' => $this->usuario->nombre_completo,
            'cargo' => $this->usuario->rol === 'MEDICO'
                ? ($this->usuario->especialidad_nombre ?? $this->usuario->cargo)
                : $this->usuario->cargo,
            'especialidad' => $this->usuario->especialidad_nombre,
            'area' => $this->area,
        ];
    }
}
