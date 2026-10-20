<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;

class HistorialClinico extends HospitalModel
{
    protected $table = 'historial_clinico';

    protected $primaryKey = 'id_historial';

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente');
    }
}
