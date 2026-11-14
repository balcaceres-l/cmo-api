<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class ResendMailService
{
    private string $apiKey;
    private string $fromAddress;

    public function __construct()
    {
        $this->apiKey = (string) (config('services.resend.key') ?? env('RESEND_API_KEY', ''));
        $this->fromAddress = (string) (config('services.resend.from') ?? env('RESEND_FROM_ADDRESS', 'onboarding@resend.dev'));
    }

    /**
     * Envía un código OTP al correo del paciente a través de la API de Resend.
     *
     * @param string $destinatario Correo electrónico del paciente
     * @param string $otp Código de verificación de 6 dígitos
     * @param string|null $nombre Nombre del paciente
     * @return bool
     */
    public function enviarOtp(string $destinatario, string $otp, ?string $nombre = null): bool
    {
        $nombreMostrar = $nombre ? htmlspecialchars($nombre, ENT_QUOTES, 'UTF-8') : 'Paciente';
        $asunto = 'Código de verificación: ' . $otp . ' - Centro Médico de Occidente';

        $html = <<<HTML
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f6f8fa; margin: 0; padding: 20px; color: #2d3748; }
                .card { max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 14px; padding: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { text-align: center; margin-bottom: 24px; }
                .title { font-size: 20px; font-weight: 700; color: #005a4e; margin-bottom: 8px; }
                .subtitle { font-size: 14px; color: #718096; }
                .code-box { background: #f0fdf9; border: 2px dashed #008774; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0; }
                .code { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #005a4e; font-family: monospace; }
                .notice { font-size: 13px; color: #718096; line-height: 1.5; text-align: center; }
                .footer { text-align: center; margin-top: 24px; font-size: 12px; color: #a0aec0; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="header">
                    <div class="title">Centro Médico de Occidente</div>
                    <div class="subtitle">Vinculación de Expediente Clínico</div>
                </div>
                <p>Hola <strong>{$nombreMostrar}</strong>,</p>
                <p>Hemos recibido una solicitud para vincular tu expediente clínico a tu cuenta de la aplicación móvil.</p>
                <p>Ingresa el siguiente código de seguridad en tu dispositivo para confirmar la vinculación:</p>
                <div class="code-box">
                    <div class="code">{$otp}</div>
                </div>
                <p class="notice">Este código tiene una vigencia de <strong>15 minutos</strong> y puede utilizarse una sola vez. Si tú no solicitaste este código, por favor ignora este mensaje.</p>
                <div class="footer">
                    &copy; 2026 Centro Médico de Occidente. Todos los derechos reservados.
                </div>
            </div>
        </body>
        </html>
        HTML;

        $texto = "Centro Médico de Occidente\n\n"
            . "Hola {$nombreMostrar},\n\n"
            . "Tu código de verificación para vincular tu expediente clínico es: {$otp}\n\n"
            . "Este código vence en 15 minutos. Si no solicitaste este código, puedes ignorar este correo.\n";

        // Registro de respaldo en los logs locales de Laravel
        Log::info("OTP generado para vincular expediente ({$destinatario}): {$otp}");

        try {
            $from = "Centro Médico de Occidente <{$this->fromAddress}>";
            $response = Http::withToken($this->apiKey)
                ->timeout(10)
                ->post('https://api.resend.com/emails', [
                    'from' => $from,
                    'to' => [$destinatario],
                    'subject' => $asunto,
                    'html' => $html,
                    'text' => $texto,
                ]);

            if ($response->successful()) {
                Log::info("Correo OTP enviado con éxito a {$destinatario} vía Resend (ID: " . ($response->json('id') ?? 'N/A') . ")");
                return true;
            }

            Log::warning("Resend API devolvió estado " . $response->status() . ": " . $response->body());
            // Si Resend falla (por ejemplo, si el correo destinatario no está verificado en modo prueba),
            // registramos el error pero permitimos continuar en local gracias al log.
            return false;
        } catch (\Throwable $e) {
            Log::error("Error conectando con la API de Resend: " . $e->getMessage());
            return false;
        }
    }
}
