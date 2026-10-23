<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;

class OrdenLaboratorio extends HospitalModel
{
    protected $table = 'orden_laboratorio';

    protected $primaryKey = 'id_orden';

    protected function casts(): array
    {
        return ['fecha_solicitud' => 'datetime', 'fecha_inicio' => 'datetime', 'fecha_resultado' => 'datetime'];
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente');
    }

    public function examen(): BelongsTo
    {
        return $this->belongsTo(ExamenCatalogo::class, 'id_examen');
    }

    public function consulta(): BelongsTo
    {
        return $this->belongsTo(Consulta::class, 'id_consulta');
    }
}
