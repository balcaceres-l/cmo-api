<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\BelongsTo;

class TurnoPersonal extends HospitalModel
{
    protected $table = 'turno_personal';

    protected $primaryKey = 'id_turno';

    protected function casts(): array
    {
        return ['fecha' => 'date:Y-m-d'];
    }

    public function usuario(): BelongsTo
    {
        return $this->belongsTo(User::class, 'id_usuario');
    }
}
