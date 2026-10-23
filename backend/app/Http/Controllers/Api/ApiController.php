<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Concerns\RespondsWithJson;
use App\Http\Controllers\Controller;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Foundation\Auth\Access\AuthorizesRequests;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;
use Illuminate\Validation\Rules\Exists;

abstract class ApiController extends Controller
{
    use AuthorizesRequests,RespondsWithJson;

    protected function paginar(Builder $q, Request $r)
    {
        return $q->paginate(min(max((int) $r->input('per_page', 15), 1), 100));
    }

    /** Un `id_medico` debe ser un médico activo, no cualquier usuario. */
    protected function reglaMedico(): Exists
    {
        return Rule::exists('users', 'id')->where('rol', 'MEDICO')->where('estado', 'ACTIVO');
    }

    /** Consultorio de la planta 1 que esté en uso. */
    protected function reglaConsultorio(): Exists
    {
        return Rule::exists('consultorio', 'id_consultorio')->where('activo', true);
    }

    /** Un turno o una asignación de personal no puede apuntar a una cuenta de paciente. */
    protected function reglaPersonal(): Exists
    {
        return Rule::exists('users', 'id')->whereNot('rol', 'PACIENTE')->where('estado', 'ACTIVO');
    }
}
