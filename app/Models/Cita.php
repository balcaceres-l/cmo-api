<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasOne;

class Cita extends HospitalModel
{
    protected $table = 'cita';

    protected $primaryKey = 'id_cita';

    protected function casts(): array
    {
        return ['fecha' => 'date', 'hora_llegada' => 'datetime'];
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente');
    }

    public function medico(): BelongsTo
    {
        return $this->belongsTo(User::class, 'id_medico');
    }

    public function consulta(): HasOne
    {
        return $this->hasOne(Consulta::class, 'id_cita');
    }
}
