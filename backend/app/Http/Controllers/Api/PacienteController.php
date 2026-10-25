<?php

namespace App\Http\Controllers\Api;

use App\Exceptions\ConflictoException;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Services\ActivacionCuentaService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

class PacienteController extends ApiController
{
    public function index(Request $r)
    {
        $q = Paciente::query()->when($r->user()->rol === 'MEDICO', fn ($q) => $q->tratadoPor($r->user()->id))->when($r->q, fn ($q, $v) => $q->where(fn ($x) => $x->where('nombre_completo', 'like', "%$v%")->orWhere('dui', 'like', "%$v%")->orWhere('numero_expediente', 'like', "%$v%")))->orderBy('nombre_completo');

        return $this->success($this->paginar($q, $r));
    }

    public function store(Request $r)
    {
        $d = $this->validar($r);

        return DB::transaction(function () use ($d, $r) {
            $d['numero_expediente'] = $this->generarExpediente();
            $d['id_registrado_por'] = $r->user()->id;
            $d['fecha_registro'] = now();
            $p = Paciente::create($d);
            HistorialClinico::create(['id_paciente' => $p->getKey()]);

            return $this->success($p, 'Paciente creado.', 201);
        });
    }

    public function show(Paciente $paciente)
    {
        $this->authorize('view', $paciente);

        return $this->success($paciente);
    }

    public function update(Request $r, Paciente $paciente)
    {
        $paciente->update($this->validar($r, $paciente, true));

        return $this->success($paciente->refresh());
    }

    /** Recepción entrega este código al paciente para que vincule su cuenta al expediente. */
    public function codigoActivacion(Paciente $paciente, ActivacionCuentaService $activacion)
    {
        if ($paciente->id_usuario) {
            throw new ConflictoException('El paciente ya tiene una cuenta vinculada.');
        }

        return $this->success($activacion->generar($paciente), 'Código de activación generado.', 201);
    }

    public function expediente(Paciente $paciente)
    {
        $this->authorize('viewClinical', $paciente);
        $paciente->load(['historial', 'citas.consulta.recetas', 'ordenesLaboratorio.examen', 'ingresos.cama.habitacion']);

        return $this->success($paciente);
    }

    public function historial(Request $r, Paciente $paciente)
    {
        $this->authorize('updateClinical', $paciente);
        $d = $r->validate(['antecedentes_personales' => 'nullable|string', 'antecedentes_familiares' => 'nullable|string']);
        $paciente->historial()->updateOrCreate(['id_paciente' => $paciente->getKey()], $d);

        return $this->success($paciente->historial);
    }

    public function miExpediente(Request $r)
    {
        $p = $r->user()->paciente()->firstOrFail();
        // De cada médico solo el nombre y el cargo: nada de usuario de acceso, correo ni teléfono del personal.
        $p->load(['historial', 'citas.medico:id,nombre_completo,cargo', 'citas.consulta.recetas', 'ordenesLaboratorio.examen', 'ingresos.cama.habitacion', 'ingresos.medico:id,nombre_completo,cargo']);

        return $this->success($p);
    }

    private function validar(Request $r, ?Paciente $p = null, bool $parcial = false): array
    {
        $req = $parcial ? 'sometimes' : 'required';

        return $r->validate(['nombre_completo' => "$req|string|max:150", 'fecha_nacimiento' => "$req|date|before:today", 'dui' => ['nullable', 'string', 'max:12', Rule::unique('paciente', 'dui')->ignore($p?->getKey(), 'id_paciente')], 'sexo' => [$req, Rule::in(['M', 'F'])], 'telefono' => 'nullable|string|max:25', 'direccion' => 'nullable|string', 'tipo_sangre' => 'nullable|string|max:5', 'alergias' => 'nullable|string']);
    }

    private function generarExpediente(): string
    {
        $y = now()->format('Y');
        $last = Paciente::where('numero_expediente', 'like', "EXP-$y-%")->lockForUpdate()->max('numero_expediente');

        return sprintf('EXP-%s-%06d', $y, $last ? (int) substr($last, -6) + 1 : 1);
    }
}
