<?php

namespace App\Services;

use App\Models\Paciente;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;

/**
 * Código de un solo uso para que un paciente que ya tiene expediente vincule
 * su cuenta. Sin él, bastaría conocer un DUI para leer el expediente ajeno.
 */
class ActivacionCuentaService
{
    public const VIGENCIA_HORAS = 24;

    public const MAX_INTENTOS = 5;

    /** @return array{codigo: string, vence_en: string} */
    public function generar(Paciente $paciente): array
    {
        $codigo = (string) random_int(100000, 999999);
        $vence = now()->addHours(self::VIGENCIA_HORAS);
        Cache::put($this->clave($paciente), [
            'hash' => Hash::make($codigo),
            'intentos' => 0,
            'vence' => $vence->getTimestamp(),
        ], $vence);

        return ['codigo' => $codigo, 'vence_en' => $vence->toIso8601String()];
    }

    /** Devuelve true y lo invalida si el código es correcto; cuenta los intentos fallidos. */
    public function consumir(Paciente $paciente, ?string $codigo): bool
    {
        $datos = Cache::get($this->clave($paciente));
        if (! $datos || ! $codigo) {
            return false;
        }

        if (Hash::check($codigo, $datos['hash'])) {
            Cache::forget($this->clave($paciente));

            return true;
        }

        $datos['intentos']++;
        if ($datos['intentos'] >= self::MAX_INTENTOS) {
            Cache::forget($this->clave($paciente));
        } else {
            Cache::put($this->clave($paciente), $datos, Carbon::createFromTimestamp($datos['vence']));
        }

        return false;
    }

    private function clave(Paciente $paciente): string
    {
        return 'activacion-cuenta:'.$paciente->getKey();
    }
}
