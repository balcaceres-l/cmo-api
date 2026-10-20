<?php

namespace App\Policies;

use App\Models\Paciente;
use App\Models\User;

class PacientePolicy
{
    use ClinicalAccess;

    public function view(User $u, Paciente $p): bool
    {
        return in_array($u->rol, ['ADMINISTRADOR', 'RECEPCIONISTA']) || $this->clinico($u, $p);
    }

    public function viewClinical(User $u, Paciente $p): bool
    {
        return $this->clinico($u, $p);
    }

    public function updateClinical(User $u, Paciente $p): bool
    {
        return $u->rol === 'ADMINISTRADOR' || ($u->rol === 'MEDICO' && $this->tratante($u, $p));
    }
}
