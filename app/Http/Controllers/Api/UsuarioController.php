<?php

namespace App\Http\Controllers\Api;

use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class UsuarioController extends ApiController
{
    public function index(Request $r)
    {
        return $this->success($this->paginar(User::query()->orderBy('nombre_completo'), $r));
    }

    public function medicos(Request $r)
    {
        return $this->success($this->paginar(User::where('rol', 'MEDICO')->where('estado', 'ACTIVO')->orderBy('nombre_completo'), $r));
    }

    public function store(Request $r)
    {
        $d = $this->validar($r);
        if ($d['rol'] === 'PACIENTE') {
            return $this->failure('El administrador solo puede crear personal.', 422);
        }

return $this->success(User::create($d), 'Usuario creado.', 201);
    }

    public function show(User $usuario)
    {
        return $this->success($usuario);
    }

    public function update(Request $r, User $usuario)
    {
        $d = $this->validar($r, $usuario, true);
        if (($d['rol'] ?? $usuario->rol) === 'PACIENTE') {
            return $this->failure('El administrador solo gestiona personal.', 422);
        }$usuario->update($d);

        return $this->success($usuario->refresh(), 'Usuario actualizado.');
    }

    public function cambiarEstado(Request $r, User $usuario)
    {
        $d = $r->validate(['estado' => ['required', Rule::in(['ACTIVO', 'INACTIVO'])]]);
        $usuario->update($d);

        return $this->success($usuario);
    }

    private function validar(Request $r, ?User $u = null, bool $parcial = false): array
    {
        $req = $parcial ? 'sometimes' : 'required';

        return $r->validate(['nombre_completo' => "$req|string|max:150", 'usuario' => [$req, 'string', 'max:80', Rule::unique('users', 'usuario')->ignore($u)], 'email' => ['nullable', 'email', Rule::unique('users', 'email')->ignore($u)], 'password' => ($parcial ? 'nullable' : 'required').'|string|min:8', 'rol' => [$req, Rule::in(['ADMINISTRADOR', 'MEDICO', 'RECEPCIONISTA', 'PACIENTE'])], 'cargo' => 'nullable|string|max:100', 'telefono' => 'nullable|string|max:25', 'estado' => ['sometimes', Rule::in(['ACTIVO', 'INACTIVO'])]]);
    }
}
