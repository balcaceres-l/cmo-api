<?php

namespace App\Http\Controllers\Api;

use App\Models\Cama;
use App\Models\Habitacion;
use App\Models\Ingreso;
use App\Models\Paciente;
use App\Models\User;
use App\Services\AsignacionCamaService;
use Illuminate\Http\Request;

class HospitalizacionController extends ApiController
{
    public function __construct(private AsignacionCamaService $servicio) {}

    public function habitaciones(Request $r)
    {
        $habitaciones = Habitacion::with('camas.ingresoActivo.paciente')->orderBy('planta')->orderBy('codigo')->get();
        $mapa = $habitaciones->groupBy('planta')->map(fn ($grupo) => $grupo->map(function ($h) use ($r) {
            return ['codigo' => $h->codigo, 'tipo' => $h->tipo, 'capacidad' => $h->capacidad, 'camas' => $h->camas->map(function ($c) use ($r) {
                $i = $c->ingresoActivo;
                $puede = $i && (in_array($r->user()->rol, ['ADMINISTRADOR', 'RECEPCIONISTA']) || ($r->user()->rol === 'MEDICO' && $i->id_medico === $r->user()->id));

                return ['codigo' => $c->codigo, 'estado' => $c->estado, 'paciente' => $puede ? ['id_paciente' => $i->paciente->getKey(), 'nombre_completo' => $i->paciente->nombre_completo, 'numero_expediente' => $i->paciente->numero_expediente] : null];
            })->values()];
        })->values());

        return $this->success($mapa);
    }

    public function ingresos(Request $r)
    {
        $q = Ingreso::with(['paciente', 'cama.habitacion', 'medico'])->when($r->boolean('activos'), fn ($q) => $q->whereNull('fecha_alta'))->orderByDesc('fecha_ingreso');
        if ($r->user()->rol === 'MEDICO') {
            $q->where('id_medico', $r->user()->id);
        }

return $this->success($this->paginar($q, $r));
    }

    public function ingresar(Request $r)
    {
        $d = $r->validate(['id_paciente' => 'required|exists:paciente,id_paciente', 'tipo' => 'required|in:VIP,SEMIPRIVADA', 'id_cama' => 'nullable|exists:cama,id_cama', 'id_medico' => 'required|exists:users,id', 'motivo' => 'required|string']);
        $i = $this->servicio->asignar(Paciente::findOrFail($d['id_paciente']), $d['tipo'], $d['id_cama'] ?? null, User::findOrFail($d['id_medico']), $d['motivo'], $r->user());

        return $this->success($i, 'Ingreso creado.', 201);
    }

    public function alta(Ingreso $ingreso)
    {
        return $this->success($this->servicio->alta($ingreso), 'Alta registrada.');
    }

    public function trasladar(Request $r, Ingreso $ingreso)
    {
        $d = $r->validate(['id_cama' => 'required|exists:cama,id_cama']);

        return $this->success($this->servicio->trasladar($ingreso, Cama::with('habitacion')->findOrFail($d['id_cama']), $r->user()), 'Traslado realizado.');
    }
}
