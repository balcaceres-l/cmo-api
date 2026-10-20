<?php

namespace App\Policies;

use App\Models\Consulta;
use App\Models\User;

class ConsultaPolicy
{
    use ClinicalAccess;

    public function view(User $u, Consulta $c): bool
    {
        return $this->clinico($u, $c->cita->paciente);
    }

    public function update(User $u, Consulta $c): bool
    {
        return $u->rol === 'MEDICO' && $c->id_medico === $u->id && ! $c->fecha_hora_fin;
    }
}
