<?php

namespace App\Http\Controllers\Api;

use App\Http\Resources\PublicAmbulanciaResource;
use App\Http\Resources\PublicAvisoResource;
use App\Http\Resources\PublicOcupacionResource;
use App\Http\Resources\PublicTurnoResource;
use App\Models\Ambulancia;
use App\Models\Aviso;
use App\Models\Cama;
use App\Models\TurnoPersonal;
use Illuminate\Support\Facades\DB;

class PublicoController extends ApiController
{
    public function ocupacion()
    {
        $data = Cama::join('habitacion', 'habitacion.id_habitacion', '=', 'cama.id_habitacion')->select('planta', 'tipo', 'estado', DB::raw('count(*) total'))->groupBy('planta', 'tipo', 'estado')->get();

        return $this->success(PublicOcupacionResource::collection($data));
    }

    public function medicos()
    {
        $data = TurnoPersonal::with('usuario:id,nombre_completo,cargo')->whereDate('fecha', today())->where('hora_inicio', '<=', now()->format('H:i:s'))->where('hora_fin', '>=', now()->format('H:i:s'))->whereHas('usuario', fn ($q) => $q->where('rol', 'MEDICO'))->get();

        return $this->success(PublicTurnoResource::collection($data));
    }

    public function avisos()
    {
        $data = Aviso::where('publico', true)->where('vigente_desde', '<=', now())->where(fn ($q) => $q->whereNull('vigente_hasta')->orWhere('vigente_hasta', '>=', now()))->get();

        return $this->success(PublicAvisoResource::collection($data));
    }

    public function ambulancia()
    {
        return $this->success(new PublicAmbulanciaResource(Ambulancia::firstOrFail()));
    }
}
