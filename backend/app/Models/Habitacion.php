<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\HasMany;

class Habitacion extends HospitalModel
{
    protected $table = 'habitacion';

    protected $primaryKey = 'id_habitacion';

    public $timestamps = false;

    public function camas(): HasMany
    {
        return $this->hasMany(Cama::class, 'id_habitacion');
    }
}
