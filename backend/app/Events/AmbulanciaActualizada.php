<?php

namespace App\Events;

use App\Models\Ambulancia;
use Illuminate\Broadcasting\Channel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcastNow;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class AmbulanciaActualizada implements ShouldBroadcastNow
{
    use Dispatchable,SerializesModels;

    public function __construct(public Ambulancia $ambulancia) {}

    public function broadcastOn(): array
    {
        return [new Channel('ambulancia')];
    }

    public function broadcastWith(): array
    {
        $s = $this->ambulancia->servicios()->where('estado', 'EN_CURSO')->first();

        return ['estado' => $this->ambulancia->estado, 'servicio_activo' => $s ? ['prioridad' => $s->prioridad, 'hora_salida' => $s->hora_salida?->toIso8601String()] : null];
    }
}
