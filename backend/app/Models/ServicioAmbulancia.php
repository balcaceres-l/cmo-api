<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;

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

    public function reportes(): HasMany
    {
        return $this->hasMany(ReporteAmbulancia::class, 'id_servicio');
    }

    public function ultimoReporte(): HasOne
    {
        return $this->hasOne(ReporteAmbulancia::class, 'id_servicio')->latestOfMany('fecha');
    }
}
