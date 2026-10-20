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
        // Las cuentas de paciente se gestionan por registro o desde recepción;
        // convertirlas en personal mezclaría un expediente con una cuenta de staff.
        if ($usuario->rol === 'PACIENTE' || ($d['rol'] ?? $usuario->rol) === 'PACIENTE') {
            return $this->failure('El administrador solo gestiona personal.', 422);
        }
        if ($this->esSiMismo($r, $usuario) && ((isset($d['rol']) && $d['rol'] !== $usuario->rol) || ($d['estado'] ?? 'ACTIVO') === 'INACTIVO')) {
            return $this->failure('No puedes cambiar tu propio rol ni desactivar tu cuenta.', 422);
        }
        if (empty($d['password'])) {
            unset($d['password']);
        }
        $usuario->update($d);
        if ($usuario->estado === 'INACTIVO') {
            $usuario->tokens()->delete();
        }

        return $this->success($usuario->refresh(), 'Usuario actualizado.');
    }

    public function cambiarEstado(Request $r, User $usuario)
    {
        $d = $r->validate(['estado' => ['required', Rule::in(['ACTIVO', 'INACTIVO'])]]);
        if ($this->esSiMismo($r, $usuario) && $d['estado'] === 'INACTIVO') {
            return $this->failure('No puedes desactivar tu propia cuenta.', 422);
        }
        $usuario->update($d);
        if ($usuario->estado === 'INACTIVO') {
            $usuario->tokens()->delete();
        }

        return $this->success($usuario);
    }

    private function esSiMismo(Request $r, User $usuario): bool
    {
        return $r->user()->is($usuario);
    }

    private function validar(Request $r, ?User $u = null, bool $parcial = false): array
    {
        $req = $parcial ? 'sometimes' : 'required';

        return $r->validate(['nombre_completo' => "$req|string|max:150", 'usuario' => [$req, 'string', 'max:80', Rule::unique('users', 'usuario')->ignore($u)], 'email' => ['nullable', 'email', Rule::unique('users', 'email')->ignore($u)], 'password' => ($parcial ? 'nullable' : 'required').'|string|min:8', 'rol' => [$req, Rule::in(['ADMINISTRADOR', 'MEDICO', 'RECEPCIONISTA', 'PACIENTE'])], 'cargo' => 'nullable|string|max:100', 'telefono' => 'nullable|string|max:25', 'estado' => ['sometimes', Rule::in(['ACTIVO', 'INACTIVO'])]]);
    }
}
