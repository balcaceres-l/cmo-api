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
use App\Models\User;
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
        $data = TurnoPersonal::with(['usuario:id,nombre_completo,cargo,rol,id_especialidad', 'usuario.especialidad:id_especialidad,nombre'])
            ->whereDate('fecha', today())
            ->where('hora_inicio', '<=', now()->format('H:i:s'))
            ->where('hora_fin', '>=', now()->format('H:i:s'))
            ->whereHas('usuario', fn ($q) => $q->where('rol', 'MEDICO'))
            ->get();

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

    /**
     * Lista de cuentas de demostración (solo local/debug/testing) con su estado
     * de turno en vivo para pruebas de acceso en la app móvil.
     */
    public function personalDemo()
    {
        if (! app()->environment(['local', 'testing']) && ! config('app.debug')) {
            abort(404);
        }

        $ahora = now()->format('H:i:s');
        $hoy = today()->toDateString();

        $turnosHoy = TurnoPersonal::whereDate('fecha', $hoy)
            ->orderBy('hora_inicio')
            ->get()
            ->groupBy('id_usuario');

        $usuarios = User::with('especialidad:id_especialidad,nombre')
            ->whereIn('rol', ['MEDICO', 'RECEPCIONISTA'])
            ->where('estado', 'ACTIVO')
            ->get()
            ->map(function (User $u) use ($turnosHoy, $ahora) {
                $misTurnos = $turnosHoy->get($u->getKey(), collect());
                $turnoActual = $misTurnos->first(
                    fn ($t) => $t->hora_inicio <= $ahora && $t->hora_fin >= $ahora
                );
                $proximoTurno = $turnoActual
                    ? null
                    : $misTurnos->first(fn ($t) => $t->hora_inicio > $ahora);

                $esMedico = $u->rol === 'MEDICO';
                $subgrupo = $esMedico
                    ? ($u->especialidad?->nombre ?: ($u->cargo ?: 'Medicina General'))
                    : (str_contains(mb_strtolower((string) $u->cargo), 'enfermer') ? 'Enfermería' : 'Recepción');

                return [
                    'id_usuario' => $u->getKey(),
                    'usuario' => $u->usuario,
                    'nombre_completo' => $u->nombre_completo,
                    'rol' => $u->rol,
                    'cargo' => $u->cargo,
                    'especialidad' => $u->especialidad?->nombre,
                    'categoria' => $esMedico ? 'MEDICO' : 'APOYO',
                    'subgrupo' => $subgrupo,
                    'en_turno' => $turnoActual !== null,
                    'area_turno' => $turnoActual?->area,
                    'horario_turno' => $turnoActual
                        ? substr((string) $turnoActual->hora_inicio, 0, 5).'–'.substr((string) $turnoActual->hora_fin, 0, 5)
                        : ($proximoTurno ? 'Entra '.substr((string) $proximoTurno->hora_inicio, 0, 5) : null),
                ];
            })
            ->sortBy(fn ($item) => [
                $item['en_turno'] ? 0 : 1,
                mb_strtolower($item['subgrupo']),
                mb_strtolower($item['nombre_completo']),
            ])
            ->values();

        return $this->success($usuarios);
    }
}
