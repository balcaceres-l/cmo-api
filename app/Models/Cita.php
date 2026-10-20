<?php

namespace App\Models;

use App\Exceptions\ConflictoException;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasOne;

class Cita extends HospitalModel
{
    protected $table = 'cita';

    protected $primaryKey = 'id_cita';

    protected function casts(): array
    {
        return ['fecha' => 'date', 'hora_llegada' => 'datetime'];
    }

    /**
     * Corta con 409 si la cita no está en uno de los estados permitidos para
     * la acción (por ejemplo, no se reprograma una cita ya atendida).
     */
    public function exigirEstado(array $permitidos, string $accion): void
    {
        if (! in_array($this->estado, $permitidos, true)) {
            throw new ConflictoException("No se puede {$accion} una cita en estado {$this->estado}.");
        }
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente');
    }

    public function medico(): BelongsTo
    {
        return $this->belongsTo(User::class, 'id_medico');
    }

    public function consulta(): HasOne
    {
        return $this->hasOne(Consulta::class, 'id_cita');
    }
}
