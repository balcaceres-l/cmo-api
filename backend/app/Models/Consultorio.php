<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\HasMany;

class Consultorio extends HospitalModel
{
    protected $table = 'consultorio';

    protected $primaryKey = 'id_consultorio';

    public $timestamps = false;

    protected function casts(): array
    {
        return ['activo' => 'boolean'];
    }

    public function citas(): HasMany
    {
        return $this->hasMany(Cita::class, 'id_consultorio');
    }
}
