<?php

namespace App\Policies;

use App\Models\OrdenLaboratorio;
use App\Models\User;

class OrdenLaboratorioPolicy
{
    use ClinicalAccess;

    public function view(User $u, OrdenLaboratorio $o): bool
    {
        return $this->clinico($u, $o->paciente);
    }
}
