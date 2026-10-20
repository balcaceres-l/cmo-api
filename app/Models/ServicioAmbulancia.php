<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ServicioAmbulancia extends HospitalModel
{
    protected $table = 'servicio_ambulancia';

    protected $primaryKey = 'id_servicio';

    protected function casts(): array
    {
        return ['hora_solicitud' => 'datetime', 'hora_salida' => 'datetime', 'hora_cierre' => 'datetime'];
    }

    public function ambulancia(): BelongsTo
    {
        return $this->belongsTo(Ambulancia::class, 'id_ambulancia');
    }
}
