<?php

use App\Http\Controllers\Api\AmbulanciaController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\AvisoController;
use App\Http\Controllers\Api\CitaController;
use App\Http\Controllers\Api\ConsultaController;
use App\Http\Controllers\Api\ConsultorioController;
use App\Http\Controllers\Api\DashboardController;
use App\Http\Controllers\Api\HospitalizacionController;
use App\Http\Controllers\Api\InventarioController;
use App\Http\Controllers\Api\LaboratorioController;
use App\Http\Controllers\Api\PacienteController;
use App\Http\Controllers\Api\PublicoController;
use App\Http\Controllers\Api\TurnoController;
use App\Http\Controllers\Api\UsuarioController;
use Illuminate\Support\Facades\Route;

Route::prefix('publico')->group(function () {
    Route::get('ocupacion', [PublicoController::class, 'ocupacion']);
    Route::get('medicos-de-turno', [PublicoController::class, 'medicos']);
    Route::get('avisos', [PublicoController::class, 'avisos']);
    Route::get('ambulancia', [PublicoController::class, 'ambulancia']);
});
Route::post('auth/login', [AuthController::class, 'login'])->middleware('throttle:login');
Route::post('auth/registro', [AuthController::class, 'registro'])->middleware('throttle:registro');

Route::middleware(['auth:sanctum', 'activo'])->group(function () {
    Route::get('auth/me', [AuthController::class, 'me']);
    Route::post('auth/logout', [AuthController::class, 'logout']);
    Route::patch('auth/password', [AuthController::class, 'changePassword'])->middleware('throttle:6,1');
    Route::middleware('role:ADMINISTRADOR')->group(function () {
        Route::apiResource('usuarios', UsuarioController::class)->except('destroy');
        Route::patch('usuarios/{usuario}/estado', [UsuarioController::class, 'cambiarEstado']);
        Route::apiResource('turnos', TurnoController::class)->parameters(['turnos' => 'turno']);
        Route::apiResource('avisos', AvisoController::class)->except('index');
        Route::get('dashboard/metricas', [DashboardController::class, 'metricas']);
    });
    Route::get('medicos', [UsuarioController::class, 'medicos'])->middleware('role:ADMINISTRADOR,RECEPCIONISTA');
    Route::get('mi/turnos', [TurnoController::class, 'mios'])->middleware('role:MEDICO,RECEPCIONISTA');

    Route::middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA')->group(function () {
        Route::get('pacientes', [PacienteController::class, 'index']);
        Route::get('pacientes/{paciente}', [PacienteController::class, 'show']);
        Route::get('pacientes/{paciente}/expediente', [PacienteController::class, 'expediente']);
    });
    Route::middleware('role:ADMINISTRADOR,RECEPCIONISTA')->group(function () {
        Route::post('pacientes', [PacienteController::class, 'store']);
        Route::put('pacientes/{paciente}', [PacienteController::class, 'update']);
        Route::post('pacientes/{paciente}/codigo-activacion', [PacienteController::class, 'codigoActivacion']);
    });
    Route::put('pacientes/{paciente}/historial', [PacienteController::class, 'historial'])->middleware('role:ADMINISTRADOR,MEDICO');
    Route::get('mi/expediente', [PacienteController::class, 'miExpediente'])->middleware('role:PACIENTE');

    Route::middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA')->group(function () {
        Route::get('citas', [CitaController::class, 'index']);
        Route::get('agenda/disponibilidad', [CitaController::class, 'disponibilidad']);
        Route::get('consultorios', [ConsultorioController::class, 'index']);
    });
    Route::post('citas', [CitaController::class, 'store'])->middleware('role:MEDICO,RECEPCIONISTA');
    Route::middleware('role:ADMINISTRADOR,RECEPCIONISTA')->group(function () {
        Route::patch('citas/{cita}/cancelar', [CitaController::class, 'cancelar']);
        Route::patch('citas/{cita}/reprogramar', [CitaController::class, 'reprogramar']);
        Route::patch('citas/{cita}/llegada', [CitaController::class, 'registrarLlegada']);
        Route::patch('citas/{cita}/confirmar', [CitaController::class, 'confirmar']);
    });
    Route::post('solicitudes-atencion', [CitaController::class, 'solicitar'])->middleware('role:PACIENTE');
    Route::get('mi/citas', [CitaController::class, 'mias'])->middleware('role:PACIENTE');

    Route::get('consultas/{consulta}', [ConsultaController::class, 'show'])->middleware('role:ADMINISTRADOR,MEDICO,PACIENTE');
    Route::middleware('role:MEDICO')->group(function () {
        Route::post('citas/{cita}/consulta', [ConsultaController::class, 'store']);
        Route::put('consultas/{consulta}', [ConsultaController::class, 'update']);
        Route::patch('consultas/{consulta}/finalizar', [ConsultaController::class, 'finalizar']);
        Route::post('consultas/{consulta}/recetas', [ConsultaController::class, 'receta']);
        Route::delete('recetas/{receta}', [ConsultaController::class, 'eliminarReceta']);
        Route::post('consultas/{consulta}/ordenes-laboratorio', [LaboratorioController::class, 'crear']);
    });

    Route::get('examenes', [LaboratorioController::class, 'examenes'])->middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA');
    Route::get('ordenes-laboratorio', [LaboratorioController::class, 'index'])->middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA');
    Route::middleware('role:ADMINISTRADOR,RECEPCIONISTA')->group(function () {
        Route::patch('ordenes-laboratorio/{orden}/iniciar', [LaboratorioController::class, 'iniciar']);
        Route::patch('ordenes-laboratorio/{orden}/completar', [LaboratorioController::class, 'completar']);
    });
    Route::get('mi/laboratorio', [LaboratorioController::class, 'mias'])->middleware('role:PACIENTE');

    Route::get('habitaciones', [HospitalizacionController::class, 'habitaciones'])->middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA');
    Route::get('ingresos', [HospitalizacionController::class, 'ingresos'])->middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA');
    Route::middleware('role:ADMINISTRADOR,RECEPCIONISTA')->group(function () {
        Route::post('ingresos', [HospitalizacionController::class, 'ingresar']);
        Route::patch('ingresos/{ingreso}/alta', [HospitalizacionController::class, 'alta']);
        Route::patch('ingresos/{ingreso}/trasladar', [HospitalizacionController::class, 'trasladar']);
    });

    Route::get('ambulancia', [AmbulanciaController::class, 'show'])->middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA');
    Route::patch('ambulancia/estado', [AmbulanciaController::class, 'estado'])->middleware('role:ADMINISTRADOR');
    Route::post('ambulancia/servicios', [AmbulanciaController::class, 'despachar'])->middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA');
    Route::patch('ambulancia/servicios/{servicio}/finalizar', [AmbulanciaController::class, 'finalizar'])->middleware('role:ADMINISTRADOR,RECEPCIONISTA');
    Route::get('ambulancia/servicios', [AmbulanciaController::class, 'bitacora'])->middleware('role:ADMINISTRADOR,RECEPCIONISTA');
    // Reporte previo a la llegada: lo envía el personal de la ambulancia
    // (recepción/enfermería con cargo de paramédico, o el médico de emergencia).
    Route::middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA')->group(function () {
        Route::post('ambulancia/servicios/{servicio}/reportes', [AmbulanciaController::class, 'reportar']);
        Route::get('ambulancia/servicios/{servicio}/reportes', [AmbulanciaController::class, 'reportes']);
    });

    Route::get('recursos', [InventarioController::class, 'index'])->middleware('role:ADMINISTRADOR,RECEPCIONISTA');
    Route::get('recursos/{recurso}', [InventarioController::class, 'show'])->middleware('role:ADMINISTRADOR,RECEPCIONISTA');
    Route::middleware('role:ADMINISTRADOR')->group(function () {
        Route::post('recursos', [InventarioController::class, 'store']);
        Route::put('recursos/{recurso}', [InventarioController::class, 'update']);
        Route::delete('recursos/{recurso}', [InventarioController::class, 'destroy']);
    });
    Route::post('recursos/{recurso}/movimientos', [InventarioController::class, 'movimiento'])->middleware('role:ADMINISTRADOR,RECEPCIONISTA');
    Route::get('avisos', [AvisoController::class, 'index'])->middleware('role:ADMINISTRADOR,MEDICO,RECEPCIONISTA');
});
