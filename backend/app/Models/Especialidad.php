<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\HasMany;

class Especialidad extends HospitalModel
{
    protected $table = 'especialidad';

    protected $primaryKey = 'id_especialidad';

    public $timestamps = true;

    protected function casts(): array
    {
        return [
            'es_sistema' => 'boolean',
            'activo' => 'boolean',
        ];
    }

    public function medicos(): HasMany
    {
        return $this->hasMany(User::class, 'id_especialidad', 'id_especialidad')
            ->where('rol', 'MEDICO');
    }

    public function citas(): HasMany
    {
        return $this->hasMany(Cita::class, 'id_especialidad', 'id_especialidad');
    }
}
