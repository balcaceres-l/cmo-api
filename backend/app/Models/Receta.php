<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Receta extends HospitalModel
{
    protected $table = 'receta';

    protected $primaryKey = 'id_receta';

    public function consulta(): BelongsTo
    {
        return $this->belongsTo(Consulta::class, 'id_consulta');
    }
}
