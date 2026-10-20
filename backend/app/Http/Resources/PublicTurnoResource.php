<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class PublicTurnoResource extends JsonResource
{
    public function toArray(Request $r): array
    {
        return ['nombre' => $this->usuario->nombre_completo, 'cargo' => $this->usuario->cargo, 'area' => $this->area];
    }
}
