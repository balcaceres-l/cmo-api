<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;

class SignoVital extends HospitalModel
{
    protected $table = 'signo_vital';

    protected $primaryKey = 'id_signo';

    protected function casts(): array
    {
        return ['temperatura_c' => 'float', 'peso_kg' => 'float', 'talla_cm' => 'float'];
    }

    public function cita(): BelongsTo
    {
        return $this->belongsTo(Cita::class, 'id_cita');
    }
}
