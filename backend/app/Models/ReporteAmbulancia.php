<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ReporteAmbulancia extends HospitalModel
{
    protected $table = 'reporte_ambulancia';

    protected $primaryKey = 'id_reporte';

    public $timestamps = false;

    protected function casts(): array
    {
        return ['llegada_estimada' => 'datetime', 'fecha' => 'datetime'];
    }

    public function servicio(): BelongsTo
    {
        return $this->belongsTo(ServicioAmbulancia::class, 'id_servicio');
    }

    public function autor(): BelongsTo
    {
        return $this->belongsTo(User::class, 'id_autor');
    }
}
