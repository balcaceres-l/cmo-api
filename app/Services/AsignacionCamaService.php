<?php

namespace App\Services;

use App\Events\OcupacionActualizada;
use App\Exceptions\ConflictoException;
use App\Models\Cama;
use App\Models\Ingreso;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Support\Facades\DB;

class AsignacionCamaService
{
    public function asignar(Paciente $paciente, string $tipo, ?string $idCama, User $medico, string $motivo, ?User $registradoPor = null): Ingreso
    {
        return DB::transaction(function () use ($paciente, $tipo, $idCama, $medico, $motivo, $registradoPor) {
            $camas = Cama::query()->with('habitacion')->whereHas('habitacion', fn ($q) => $q->where('tipo', $tipo))->lockForUpdate()->get();
            if (Ingreso::where('id_paciente', $paciente->getKey())->whereNull('fecha_alta')->exists()) {
                throw new ConflictoException('El paciente ya tiene un ingreso activo.');
            }$cama = $idCama ? $camas->firstWhere('id_cama', $idCama) : $camas->where('estado', 'DISPONIBLE')->sortBy(function ($c) use ($tipo) {
                if ($tipo !== 'SEMIPRIVADA') {
                    return $c->codigo;
                }$ocupadas = Cama::where('id_habitacion', $c->id_habitacion)->where('estado', 'OCUPADA')->count();

                return ($ocupadas ? 0 : 1).$c->codigo;
            })->first();
            if (! $cama || $cama->estado !== 'DISPONIBLE') {
                throw new ConflictoException($idCama ? 'La cama seleccionada está ocupada.' : 'No hay camas disponibles en planta '.($tipo === 'VIP' ? 3 : 2));
            }try {
                $ingreso = Ingreso::create(['id_paciente' => $paciente->getKey(), 'id_cama' => $cama->getKey(), 'id_medico' => $medico->getKey(), 'fecha_ingreso' => now(), 'motivo' => $motivo, 'id_registrado_por' => ($registradoPor ?? $medico)->getKey()]);
                $cama->update(['estado' => 'OCUPADA']);
                OcupacionActualizada::dispatch();

                return $ingreso;
            } catch (UniqueConstraintViolationException) {
                throw new ConflictoException('La cama o el paciente ya tiene un ingreso activo.');
            }
        });
    }

    public function alta(Ingreso $ingreso): Ingreso
    {
        return DB::transaction(function () use ($ingreso) {
            $ingreso = Ingreso::lockForUpdate()->findOrFail($ingreso->getKey());
            if ($ingreso->fecha_alta) {
                throw new ConflictoException('El ingreso ya fue dado de alta.');
            }

            $ingreso->update(['fecha_alta' => now()]);
            $ingreso->cama()->update(['estado' => 'DISPONIBLE']);
            OcupacionActualizada::dispatch();

            return $ingreso->refresh();
        });
    }

    public function trasladar(Ingreso $ingreso, Cama $destino, User $actor): Ingreso
    {
        return DB::transaction(function () use ($ingreso, $destino, $actor) {
            $paciente = $ingreso->paciente;
            $medico = $ingreso->medico;
            $motivo = 'Traslado: '.$ingreso->motivo;
            $tipo = $destino->habitacion->tipo;
            $this->alta($ingreso);

            return $this->asignar($paciente, $tipo, $destino->getKey(), $medico, $motivo, $actor);
        });
    }
}
