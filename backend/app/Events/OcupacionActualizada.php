<?php

namespace App\Events;

use Illuminate\Broadcasting\Channel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcastNow;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class OcupacionActualizada implements ShouldBroadcastNow
{
    use Dispatchable,SerializesModels;

    public function broadcastOn(): array
    {
        return [new Channel('ocupacion')];
    }

    public function broadcastWith(): array
    {
        return ['actualizada_en' => now()->toIso8601String()];
    }
}
