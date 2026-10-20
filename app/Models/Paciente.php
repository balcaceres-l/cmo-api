<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;

class Paciente extends HospitalModel
{
    protected $table = 'paciente';

    protected $primaryKey = 'id_paciente';

    public $timestamps = false;

    protected function casts(): array
    {
        return ['fecha_nacimiento' => 'date', 'fecha_registro' => 'datetime'];
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
