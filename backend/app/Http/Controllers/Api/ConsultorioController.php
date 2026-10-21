<?php

namespace App\Http\Controllers\Api;

use App\Models\Consultorio;

class ConsultorioController extends ApiController
{
    /** Consultorios de consulta general (planta 1) disponibles para agendar. */
    public function index()
    {
        return $this->success(Consultorio::where('activo', true)->orderBy('codigo')->get());
    }
}
