<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Consulta extends HospitalModel
{
    protected $table = 'consulta';

    protected $primaryKey = 'id_consulta';

    protected function casts(): array
    {
        return ['fecha_hora_inicio' => 'datetime', 'fecha_hora_fin' => 'datetime'];
    }

    public function cita(): BelongsTo
    {
        return $this->belongsTo(Cita::class, 'id_cita');
    }

    public function medico(): BelongsTo
    {
        return $this->belongsTo(User::class, 'id_medico');
    }

    public function recetas(): HasMany
    {
        return $this->hasMany(Receta::class, 'id_consulta');
    }

    public function ordenesLaboratorio(): HasMany
    {
        return $this->hasMany(OrdenLaboratorio::class, 'id_consulta');
    }
}
