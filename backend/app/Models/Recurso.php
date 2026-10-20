<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\HasMany;

class Recurso extends HospitalModel
{
    protected $table = 'recurso';

    protected $primaryKey = 'id_recurso';

    protected $appends = ['bajo_minimo'];

    public function getBajoMinimoAttribute(): bool
    {
        return $this->cantidad <= $this->stock_minimo;
    }

    public function movimientos(): HasMany
    {
        return $this->hasMany(MovimientoInventario::class, 'id_recurso');
    }
}
