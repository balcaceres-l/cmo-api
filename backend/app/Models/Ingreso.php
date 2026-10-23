<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Ingreso extends HospitalModel
{
    protected $table = 'ingreso';

    protected $primaryKey = 'id_ingreso';

    protected function casts(): array
    {
        return ['fecha_ingreso' => 'datetime', 'fecha_alta' => 'datetime'];
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente');
    }

    public function cama(): BelongsTo
    {
        return $this->belongsTo(Cama::class, 'id_cama');
    }

    public function medico(): BelongsTo
    {
        return $this->belongsTo(User::class, 'id_medico');
    }
}
