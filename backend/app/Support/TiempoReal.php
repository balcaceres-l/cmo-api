<?php

namespace App\Support;

use Illuminate\Support\Facades\DB;

/**
 * Emite eventos de broadcast sin acoplar la operación al servidor WebSocket.
 *
 * - Espera al commit: si la transacción se revierte, nadie recibe un estado
 *   que nunca existió.
 * - Si Reverb no responde, se reporta el error y la operación sigue siendo
 *   válida; los clientes se recuperan consultando el endpoint de estado.
 */
class TiempoReal
{
    public static function emitir(object $evento): void
    {
        DB::afterCommit(fn () => rescue(fn () => event($evento), report: true));
    }
}
