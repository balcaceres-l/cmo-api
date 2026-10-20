<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class PublicOcupacionResource extends JsonResource
{
    public function toArray(Request $r): array
    {
        return ['planta' => (int) $this->planta, 'tipo' => $this->tipo, 'estado' => $this->estado, 'total' => (int) $this->total];
    }
}
