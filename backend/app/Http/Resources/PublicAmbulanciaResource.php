<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class PublicAmbulanciaResource extends JsonResource
{
    public function toArray(Request $r): array
    {
        return ['estado' => $this->estado];
    }
}
