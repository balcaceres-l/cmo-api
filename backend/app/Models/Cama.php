<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasOne;

class Cama extends HospitalModel
{
    protected $table = 'cama';

    protected $primaryKey = 'id_cama';

    public $timestamps = false;

    public function habitacion(): BelongsTo
    {
        return $this->belongsTo(Habitacion::class, 'id_habitacion');
    }

    public function ingresoActivo(): HasOne
    {
        return $this->hasOne(Ingreso::class, 'id_cama')->whereNull('fecha_alta');
    }
}
