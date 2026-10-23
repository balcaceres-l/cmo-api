<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Support\Facades\Gate;

class Paciente extends HospitalModel
{
    protected $table = 'paciente';

    protected $primaryKey = 'id_paciente';

    public $timestamps = false;

    /**
     * Estados de cita que convierten al médico en tratante. Las canceladas,
     * las no asistidas y las solicitudes sin confirmar no dan acceso clínico.
     */
    public const ESTADOS_CITA_TRATANTE = ['AGENDADA', 'EN_ESPERA', 'EN_ATENCION', 'ATENDIDA'];

    /**
     * Pacientes con los que el médico tiene relación clínica: una cita vigente
     * o atendida, o un ingreso hospitalario a su cargo.
     */
    public function scopeTratadoPor(Builder $q, string $medico): void
    {
        $q->where(fn ($x) => $x
            ->whereHas('citas', fn ($c) => $c->where('id_medico', $medico)->whereIn('estado', self::ESTADOS_CITA_TRATANTE))
            ->orWhereHas('ingresos', fn ($i) => $i->where('id_medico', $medico)));
    }

    /** Campos clínicos que solo ve quien pasa `viewClinical` (paciente, tratante, admin). */
    public const CAMPOS_SENSIBLES = ['tipo_sangre', 'alergias'];

    /**
     * Se filtra al serializar para que también aplique cuando el paciente viaja
     * anidado en citas, ingresos u órdenes de laboratorio.
     */
    public function toArray(): array
    {
        $data = parent::toArray();
        if (! Gate::allows('viewClinical', $this)) {
            $data = array_diff_key($data, array_flip(self::CAMPOS_SENSIBLES));
        }

        return $data;
    }

    protected function casts(): array
    {
        return ['fecha_nacimiento' => 'date:Y-m-d', 'fecha_registro' => 'datetime'];
    }

    public function usuario(): BelongsTo
    {
        return $this->belongsTo(User::class, 'id_usuario');
    }

    public function historial(): HasOne
    {
        return $this->hasOne(HistorialClinico::class, 'id_paciente');
    }

    public function citas(): HasMany
    {
        return $this->hasMany(Cita::class, 'id_paciente');
    }

    public function ingresos(): HasMany
    {
        return $this->hasMany(Ingreso::class, 'id_paciente');
    }

    public function ordenesLaboratorio(): HasMany
    {
        return $this->hasMany(OrdenLaboratorio::class, 'id_paciente');
    }
}
