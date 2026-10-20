<?php

namespace App\Policies;

use App\Models\Ingreso;
use App\Models\User;

class IngresoPolicy
{
    use ClinicalAccess;

    public function view(User $u, Ingreso $i): bool
    {
        return $u->rol === 'RECEPCIONISTA' || $this->clinico($u, $i->paciente);
    }
}
