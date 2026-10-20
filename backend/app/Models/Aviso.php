<?php

namespace App\Models;

class Aviso extends HospitalModel
{
    protected $table = 'aviso';

    protected $primaryKey = 'id_aviso';

    protected function casts(): array
    {
        return ['publico' => 'boolean', 'vigente_desde' => 'datetime', 'vigente_hasta' => 'datetime'];
    }
}
