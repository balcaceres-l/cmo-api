<?php

namespace App\Http\Controllers\Api;

use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\Rule;

class AuthController extends ApiController
{
    public function login(Request $r)
    {
        $d = $r->validate(['usuario' => 'required|string', 'password' => 'required|string']);
        $u = User::where('usuario', $d['usuario'])->first();
        if (! $u || ! Hash::check($d['password'], $u->password) || $u->estado !== 'ACTIVO') {
            return $this->failure('Credenciales incorrectas.', 401);
        }

return $this->token($u);
    }

    public function registro(Request $r)
    {
        $d = $r->validate(['nombre_completo' => 'required|string|max:150', 'usuario' => 'required|string|max:80|unique:users,usuario', 'email' => 'nullable|email|unique:users,email', 'password' => 'required|string|min:8|confirmed', 'dui' => 'required|string|max:12', 'fecha_nacimiento' => 'required|date|before:today', 'sexo' => ['required', Rule::in(['M', 'F'])], 'telefono' => 'nullable|string|max:25', 'direccion' => 'nullable|string']);

        return DB::transaction(function () use ($d) {
            $p = Paciente::where('dui', $d['dui'])->lockForUpdate()->first();
            if ($p && $p->id_usuario) {
                return $this->failure('Ya existe una cuenta asociada a este DUI.', 422, ['dui' => ['El DUI ya está registrado.']]);
            }$u = User::create(['nombre_completo' => $d['nombre_completo'], 'usuario' => $d['usuario'], 'email' => $d['email'] ?? null, 'password' => $d['password'], 'rol' => 'PACIENTE', 'telefono' => $d['telefono'] ?? null, 'estado' => 'ACTIVO']);
            if (! $p) {
                $p = Paciente::create(['numero_expediente' => $this->expediente(), 'nombre_completo' => $d['nombre_completo'], 'fecha_nacimiento' => $d['fecha_nacimiento'], 'dui' => $d['dui'], 'sexo' => $d['sexo'], 'telefono' => $d['telefono'] ?? null, 'direccion' => $d['direccion'] ?? null, 'fecha_registro' => now()]);
                HistorialClinico::create(['id_paciente' => $p->getKey()]);
            }$p->update(['id_usuario' => $u->getKey()]);

            return $this->token($u, 201);
        });
    }

    private function expediente(): string
    {
        $year = now()->format('Y');
        $last = Paciente::where('numero_expediente', 'like', "EXP-$year-%")->lockForUpdate()->max('numero_expediente');
        $n = $last ? (int) substr($last, -6) + 1 : 1;

        return sprintf('EXP-%s-%06d', $year, $n);
    }

    private function token(User $u, int $status = 200)
    {
        $u->load('paciente');

        return $this->success(['usuario' => $u, 'id_paciente' => $u->paciente?->getKey(), 'token' => $u->createToken('api')->plainTextToken, 'token_type' => 'Bearer'], 'Autenticación exitosa.', $status);
    }

    public function me(Request $r)
    {
        $u = $r->user()->load('paciente');

        return $this->success(['usuario' => $u, 'id_paciente' => $u->paciente?->getKey()]);
    }

    public function logout(Request $r)
    {
        $r->user()->currentAccessToken()?->delete();

        return $this->success(null, 'Sesión cerrada.');
    }

    public function changePassword(Request $r)
    {
        $d = $r->validate(['password_actual' => 'required', 'password' => 'required|min:8|confirmed']);
        if (! Hash::check($d['password_actual'], $r->user()->password)) {
            return $this->failure('La contraseña actual es incorrecta.', 422);
        }$r->user()->update(['password' => $d['password']]);
        $r->user()->tokens()->delete();

        return $this->success(null,'Contraseña actualizada.');
    }
}
