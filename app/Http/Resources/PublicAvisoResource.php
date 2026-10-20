<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class PublicAvisoResource extends JsonResource
{
    public function toArray(Request $r): array
    {
        return ['titulo' => $this->titulo, 'contenido' => $this->contenido, 'vigente_desde' => $this->vigente_desde, 'vigente_hasta' => $this->vigente_hasta];
    }
}
