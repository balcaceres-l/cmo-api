<?php

namespace App\Events;

use App\Models\ReporteAmbulancia;
use Illuminate\Broadcasting\PrivateChannel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcastNow;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

/**
 * Aviso a emergencia de que la ambulancia envió un reporte del paciente.
 * Va por un canal privado del personal (no por el público "ambulancia"),
 * porque el estado del paciente es dato clínico.
 */
class ReporteAmbulanciaRecibido implements ShouldBroadcastNow
{
    use Dispatchable, SerializesModels;

    public function __construct(public ReporteAmbulancia $reporte) {}

    public function broadcastOn(): array
    {
        return [new PrivateChannel('personal')];
    }

    public function broadcastWith(): array
    {
        return [
            'id_servicio' => $this->reporte->id_servicio,
            'estado_paciente' => $this->reporte->estado_paciente,
            'llegada_estimada' => $this->reporte->llegada_estimada?->toIso8601String(),
        ];
    }
}
