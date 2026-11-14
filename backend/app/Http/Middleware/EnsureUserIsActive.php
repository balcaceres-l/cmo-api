<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Sanctum valida el token pero no el estado de la cuenta: sin esto, un usuario
 * desactivado seguiría usando la API con los tokens que ya tenía.
 */
class EnsureUserIsActive
{
    public function handle(Request $request, Closure $next): Response|JsonResponse
    {
        $u = $request->user();
        if ($u && $u->fecha_salida && $u->fecha_salida->lt(today()) && $u->estado === 'ACTIVO') {
            $u->update(['estado' => 'INACTIVO']);
        }
        if ($u && ($u->estado !== 'ACTIVO' || ! $u->estaVigenteEn(today()))) {
            $u->currentAccessToken()?->delete();

            return response()->json([
                'success' => false,
                'message' => 'El usuario se encuentra inactivo.',
            ], 401);
        }

        return $next($request);
    }
}
