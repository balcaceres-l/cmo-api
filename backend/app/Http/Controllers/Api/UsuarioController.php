<?php

namespace App\Http\Controllers\Api;

use App\Models\Especialidad;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

class UsuarioController extends ApiController
{
    public function index(Request $r)
    {
        User::sincronizarVigenciasVencidas();

        return $this->success($this->paginar(User::query()->with('especialidad')->orderBy('nombre_completo'), $r));
    }

    public function medicos(Request $r)
    {
        User::sincronizarVigenciasVencidas();
        $q = User::where('rol', 'MEDICO')
            ->where('estado', 'ACTIVO')
            ->with('especialidad')
            ->orderBy('nombre_completo');
        if ($r->filled('fecha')) {
            $fecha = $r->input('fecha');
            $q->where(fn ($sub) => $sub->whereNull('fecha_ingreso')->orWhereDate('fecha_ingreso', '<=', $fecha))
                ->where(fn ($sub) => $sub->whereNull('fecha_salida')->orWhereDate('fecha_salida', '>=', $fecha));
        }
        if ($r->filled('id_especialidad')) {
            $q->where('id_especialidad', $r->input('id_especialidad'));
        }

        return $this->success($this->paginar($q, $r));
    }

    public function store(Request $r)
    {
        $d = $this->validar($r);
        if ($d['rol'] === 'PACIENTE') {
            return $this->failure('El administrador solo puede crear personal.', 422);
        }

        $d = $this->normalizarCargoYEspecialidad($d, $d['rol'], false);
        $creado = User::create($d)->load('especialidad');

        return $this->success($creado, 'Usuario creado.', 201);
    }

    public function show(User $usuario)
    {
        return $this->success($usuario->load('especialidad'));
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
        if (isset($d['rol']) && $d['rol'] !== $usuario->rol) {
            return $this->failure('No se puede cambiar el rol de un usuario existente.', 422);
        }
        if (empty($d['password'])) {
            unset($d['password']);
        }
        $d = $this->normalizarCargoYEspecialidad($d, $usuario->rol, true);
        $usuario->update($d);
        if ($usuario->estado === 'INACTIVO') {
            $usuario->tokens()->delete();
        }

        return $this->success($usuario->refresh()->load('especialidad'), 'Usuario actualizado.');
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

        return $this->success($usuario->load('especialidad'));
    }

    private function esSiMismo(Request $r, User $usuario): bool
    {
        return $r->user()->is($usuario);
    }

    private function validar(Request $r, ?User $u = null, bool $parcial = false): array
    {
        $req = $parcial ? 'sometimes' : 'required';

        return $r->validate([
            'nombre_completo' => "$req|string|max:150",
            'usuario' => [$req, 'string', 'max:80', Rule::unique('users', 'usuario')->ignore($u)],
            'email' => ['nullable', 'email', Rule::unique('users', 'email')->ignore($u)],
            'password' => ($parcial ? 'nullable' : 'required').'|string|min:8',
            'rol' => [$req, Rule::in(['ADMINISTRADOR', 'MEDICO', 'RECEPCIONISTA', 'PACIENTE'])],
            'cargo' => 'nullable|string|max:100',
            'id_especialidad' => 'nullable|uuid|exists:especialidad,id_especialidad',
            'telefono' => 'nullable|string|max:25',
            'estado' => ['sometimes', Rule::in(['ACTIVO', 'INACTIVO'])],
            'fecha_ingreso' => 'nullable|date',
            'fecha_salida' => 'nullable|date|after_or_equal:fecha_ingreso',
        ]);
    }

    private function normalizarCargoYEspecialidad(array $d, string $rol, bool $parcial): array
    {
        if ($rol === 'MEDICO') {
            $tieneId = array_key_exists('id_especialidad', $d) && ! empty($d['id_especialidad']);
            $tieneCargo = array_key_exists('cargo', $d) && trim((string) $d['cargo']) !== '';

            if ($tieneId) {
                $d['cargo'] = null;
            } elseif ($tieneCargo) {
                $nombre = trim((string) $d['cargo']);
                $esp = Especialidad::whereRaw('LOWER(nombre) = ?', [mb_strtolower($nombre)])->first()
                    ?? Especialidad::create(['nombre' => $nombre, 'es_sistema' => false, 'activo' => true]);
                $d['id_especialidad'] = $esp->id_especialidad;
                $d['cargo'] = null;
            } elseif (! $parcial) {
                throw ValidationException::withMessages([
                    'id_especialidad' => 'La especialidad es obligatoria para los médicos.',
                ]);
            }

            return $d;
        }

        if (in_array($rol, ['ADMINISTRADOR', 'RECEPCIONISTA'], true)) {
            $d['id_especialidad'] = null;
            $permitidos = User::cargosParaRol($rol);
            $enviaCargo = array_key_exists('cargo', $d);

            if (! $parcial && (! $enviaCargo || trim((string) $d['cargo']) === '')) {
                $d['cargo'] = $permitidos[0];
            } elseif ($enviaCargo) {
                $valor = trim((string) ($d['cargo'] ?? ''));
                if ($valor === '' || ! in_array($valor, $permitidos, true)) {
                    throw ValidationException::withMessages([
                        'cargo' => 'El cargo seleccionado no es válido para el rol '.$rol.'. Opciones permitidas: '.implode(', ', $permitidos).'.',
                    ]);
                }
                $d['cargo'] = $valor;
            }

            return $d;
        }

        $d['id_especialidad'] = null;
        $d['cargo'] = null;

        return $d;
    }
}
