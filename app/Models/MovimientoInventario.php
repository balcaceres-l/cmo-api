<?php

namespace App\Models;

class MovimientoInventario extends HospitalModel
{
    protected $table = 'movimiento_inventario';

    protected $primaryKey = 'id_movimiento';

    public $timestamps = false;

    protected function casts(): array
    {
        return ['fecha' => 'datetime'];
    }
}
