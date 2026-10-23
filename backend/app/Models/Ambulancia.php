<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\HasMany;

class Ambulancia extends HospitalModel
{
    protected $table = 'ambulancia';

    protected $primaryKey = 'id_ambulancia';

    public function servicios(): HasMany
    {
        return $this->hasMany(ServicioAmbulancia::class, 'id_ambulancia');
    }
}
