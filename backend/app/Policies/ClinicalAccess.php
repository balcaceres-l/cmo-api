<?php

namespace App\Policies;

use App\Models\Paciente;
use App\Models\User;

trait ClinicalAccess
{
    private function tratante(User $u, Paciente $p): bool
    {
        return Paciente::whereKey($p->getKey())->tratadoPor($u->id)->exists();
    }

    private function clinico(User $u, Paciente $p): bool
    {
        return $u->rol === 'ADMINISTRADOR' || ($u->rol === 'PACIENTE' && $p->id_usuario === $u->id) || ($u->rol === 'MEDICO' && $this->tratante($u, $p));
    }
}
