<?php

namespace App\Models;

class ExamenCatalogo extends HospitalModel
{
    protected $table = 'examen_catalogo';

    protected $primaryKey = 'id_examen';

    public $timestamps = false;

    protected function casts(): array
    {
        return ['activo' => 'boolean'];
    }
}
