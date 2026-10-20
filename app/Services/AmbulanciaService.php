<?php

namespace App\Services;

use App\Events\AmbulanciaActualizada;
use App\Exceptions\ConflictoException;
use App\Models\Ambulancia;
use App\Models\ServicioAmbulancia;
use Illuminate\Support\Facades\DB;

class AmbulanciaService
{
    public function despachar(array $datos): ServicioAmbulancia
    {
        return DB::transaction(function () use ($datos) {
            $a = Ambulancia::lockForUpdate()->firstOrFail();
            if ($a->estado !== 'DISPONIBLE') {
                throw new ConflictoException("La ambulancia no está disponible; estado actual: {$a->estado}.");
            }$s = $a->servicios()->create($datos + ['estado' => 'EN_CURSO', 'hora_solicitud' => now(), 'hora_salida' => now()]);
            $a->update(['estado' => 'EN_SERVICIO']);
            AmbulanciaActualizada::dispatch($a);

            return $s;
        });
    }

    public function finalizar(ServicioAmbulancia $s, ?string $notas = null): ServicioAmbulancia
    {
        return DB::transaction(function () use ($s, $notas) {
            $a = Ambulancia::lockForUpdate()->findOrFail($s->id_ambulancia);
            if ($s->estado !== 'EN_CURSO') {
                throw new ConflictoException('El servicio no está en curso.');
            }$s->update(['estado' => 'FINALIZADO', 'hora_cierre' => now(), 'notas' => $notas]);
            $a->update(['estado' => 'DISPONIBLE']);
            AmbulanciaActualizada::dispatch($a);

            return $s->refresh();
        });
    }

    public function cambiarEstado(string $estado): Ambulancia
    {
        return DB::transaction(function () use ($estado) {
            $a = Ambulancia::lockForUpdate()->firstOrFail();
            if ($a->servicios()->where('estado', 'EN_CURSO')->exists()) {
                throw new ConflictoException('No se puede cambiar el estado con un servicio en curso.');
            }if (! in_array([$a->estado, $estado], [['DISPONIBLE', 'EN_MANTENIMIENTO'], ['EN_MANTENIMIENTO', 'DISPONIBLE']], true)) {
                throw new ConflictoException('Transición de estado no permitida.');
            }$a->update(['estado' => $estado]);
            AmbulanciaActualizada::dispatch($a);

            return $a;
        });
    }
}
