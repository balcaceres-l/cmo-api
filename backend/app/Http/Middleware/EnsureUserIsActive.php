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
        if ($request->user() && $request->user()->estado !== 'ACTIVO') {
            $request->user()->currentAccessToken()?->delete();

            return response()->json([
                'success' => false,
                'message' => 'El usuario se encuentra inactivo.',
            ], 401);
        }

        return $next($request);
    }
}
