<?php

namespace App\Http\Controllers\Api;

use App\Models\Paciente;
use App\Services\ResendMailService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;

class VinculacionExpedienteController extends ApiController
{
    /**
     * Solicita un código OTP enviado al correo para vincular un expediente clínico existente.
     * Solo disponible para cuentas con email y sin expediente previo.
     */
    public function solicitarOtp(Request $r, ResendMailService $resend)
    {
        $d = $r->validate([
            'dui' => 'required|string|max:12',
        ]);

        $u = $r->user();

        // Condición 1: La cuenta debe tener un correo electrónico registrado
        if (empty($u->email)) {
            return $this->failure(
                'Tu cuenta no tiene un correo electrónico registrado. Acude a recepción para obtener un código de activación en persona.',
                422,
                ['email' => ['No hay correo electrónico asociado a esta cuenta.']]
            );
        }

        // Condición 2: La cuenta no debe estar vinculada todavía a un expediente
        if ($u->paciente()->exists()) {
            return $this->failure(
                'Tu cuenta ya se encuentra vinculada a un expediente clínico.',
                409,
                ['cuenta' => ['La cuenta ya tiene un expediente asociado.']]
            );
        }

        // Buscar el expediente por DUI
        $paciente = Paciente::where('dui', $d['dui'])->first();

        if (! $paciente) {
            return $this->failure(
                'No se encontró ningún expediente clínico registrado con el DUI ingresado.',
                404,
                ['dui' => ['El DUI no corresponde a ningún paciente registrado.']]
            );
        }

        if ($paciente->id_usuario) {
            return $this->failure(
                'El expediente asociado a este DUI ya se encuentra vinculado a otra cuenta de usuario.',
                409,
                ['dui' => ['Este expediente ya tiene una cuenta asignada.']]
            );
        }

        // Generar OTP de 6 dígitos numéricos
        $otp = sprintf('%06d', random_int(100000, 999999));
        $ttl = now()->addMinutes(15);

        Cache::put("otp-vincular:{$u->id}", [
            'otp' => Hash::make($otp),
            'dui' => $paciente->dui,
            'id_paciente' => $paciente->getKey(),
            'intentos' => 0,
        ], $ttl);

        // Envío de correo mediante Resend
        $resend->enviarOtp($u->email, $otp, $u->nombre_completo);

        return $this->success([
            'email_enmascarado' => $this->enmascararCorreo($u->email),
            'vence_en_minutos' => 15,
        ], 'Código de verificación enviado a tu correo electrónico.');
    }

    /**
     * Valida el código OTP y enlaza el expediente clínico a la cuenta del paciente.
     */
    public function confirmarOtp(Request $r)
    {
        $d = $r->validate([
            'dui' => 'required|string|max:12',
            'otp' => 'required|string|size:6',
        ]);

        $u = $r->user();

        if ($u->paciente()->exists()) {
            return $this->failure(
                'Tu cuenta ya se encuentra vinculada a un expediente clínico.',
                409
            );
        }

        $claveCache = "otp-vincular:{$u->id}";
        $datos = Cache::get($claveCache);

        if (! $datos || $datos['dui'] !== $d['dui']) {
            return $this->failure(
                'El código de verificación ha vencido o no ha sido solicitado.',
                422,
                ['otp' => ['Código inválido o expirado. Solicita uno nuevo.']]
            );
        }

        if (! Hash::check($d['otp'], $datos['otp'])) {
            $datos['intentos']++;
            if ($datos['intentos'] >= 5) {
                Cache::forget($claveCache);
                return $this->failure(
                    'Demasiados intentos fallidos. Por seguridad, el código fue cancelado. Solicita uno nuevo.',
                    422,
                    ['otp' => ['Superaste el límite de intentos.']]
                );
            }
            Cache::put($claveCache, $datos, now()->addMinutes(15));

            return $this->failure(
                'El código de verificación ingresado es incorrecto.',
                422,
                ['otp' => ['El código es incorrecto.']]
            );
        }

        // Código válido: vinculación exitosa
        $paciente = Paciente::where('id_paciente', $datos['id_paciente'])->firstOrFail();
        $paciente->update(['id_usuario' => $u->id]);

        // Eliminar código usado
        Cache::forget($claveCache);

        return $this->success([
            'id_paciente' => $paciente->getKey(),
            'numero_expediente' => $paciente->numero_expediente,
            'nombre_completo' => $paciente->nombre_completo,
        ], 'Expediente clínico vinculado exitosamente.');
    }

    /**
     * Enmascara el correo para mostrarlo de forma segura en la UI (ej. j***z@hospital.edu.sv)
     */
    private function enmascararCorreo(string $email): string
    {
        $partes = explode('@', $email);
        if (count($partes) !== 2) {
            return $email;
        }

        $nombre = $partes[0];
        $dominio = $partes[1];

        $len = strlen($nombre);
        if ($len <= 2) {
            $enmascarado = substr($nombre, 0, 1) . '*';
        } else {
            $enmascarado = substr($nombre, 0, 1) . str_repeat('*', max(1, $len - 2)) . substr($nombre, -1);
        }

        return $enmascarado . '@' . $dominio;
    }
}
