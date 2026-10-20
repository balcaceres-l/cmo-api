<?php

namespace App\Http\Controllers\Api;

use App\Models\Ambulancia;
use App\Models\Cita;
use App\Models\OrdenLaboratorio;
use App\Models\Paciente;
use App\Models\Recurso;
use App\Models\ServicioAmbulancia;
use App\Models\User;
use Illuminate\Support\Facades\DB;

class DashboardController extends ApiController
{
    public function metricas()
    {
        return $this->success(['ocupacion' => DB::table('cama')->join('habitacion', 'habitacion.id_habitacion', '=', 'cama.id_habitacion')->select('planta', 'estado', DB::raw('count(*) total'))->groupBy('planta', 'estado')->get(), 'citas_hoy' => Cita::whereDate('fecha', today())->select('estado', DB::raw('count(*) total'))->groupBy('estado')->get(), 'laboratorio' => OrdenLaboratorio::select('estado', DB::raw('count(*) total'))->groupBy('estado')->get(), 'recursos_bajo_minimo' => Recurso::whereColumn('cantidad', '<=', 'stock_minimo')->count(), 'ambulancia' => ['estado' => Ambulancia::value('estado'), 'servicios_mes' => ServicioAmbulancia::whereBetween('hora_solicitud', [now()->startOfMonth(), now()->endOfMonth()])->count()], 'pacientes' => Paciente::count(), 'personal_activo' => User::where('rol', '!=', 'PACIENTE')->where('estado', 'ACTIVO')->count()]);
    }
}
