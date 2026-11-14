<?php

namespace App\Http\Controllers\Api;

use App\Models\Especialidad;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class EspecialidadController extends ApiController
{
    public function index(Request $r)
    {
        $q = Especialidad::withCount('medicos')->orderBy('nombre');
        if ($r->boolean('activas')) {
            $q->where('activo', true);
        }

        return $this->success($q->get());
    }

    public function store(Request $r)
    {
        $this->exigirAdminGeneral($r);

        $d = $r->validate([
            'nombre' => 'required|string|max:100|unique:especialidad,nombre',
            'descripcion' => 'nullable|string|max:255',
            'activo' => 'sometimes|boolean',
        ]);

        $especialidad = Especialidad::create([
            'nombre' => trim($d['nombre']),
            'descripcion' => isset($d['descripcion']) ? trim($d['descripcion']) ?: null : null,
            'es_sistema' => false,
            'activo' => $d['activo'] ?? true,
        ]);

        return $this->success($especialidad->loadCount('medicos'), 'Especialidad creada.', 201);
    }

    public function update(Request $r, Especialidad $especialidad)
    {
        $this->exigirAdminGeneral($r);

        if ($especialidad->es_sistema) {
            if ($r->filled('nombre') && trim((string) $r->input('nombre')) !== $especialidad->nombre) {
                return $this->failure('No se puede renombrar una especialidad base del sistema.', 422);
            }
            if ($r->has('activo') && ! $r->boolean('activo')) {
                return $this->failure('No se puede desactivar una especialidad base del sistema.', 422);
            }
        }

        $d = $r->validate([
            'nombre' => ['sometimes', 'required', 'string', 'max:100', Rule::unique('especialidad', 'nombre')->ignore($especialidad->getKey(), 'id_especialidad')],
            'descripcion' => 'nullable|string|max:255',
            'activo' => 'sometimes|boolean',
        ]);

        if (isset($d['nombre'])) {
            $d['nombre'] = trim($d['nombre']);
        }
        if (array_key_exists('descripcion', $d)) {
            $d['descripcion'] = $d['descripcion'] !== null ? (trim($d['descripcion']) ?: null) : null;
        }

        $especialidad->update($d);

        return $this->success($especialidad->refresh()->loadCount('medicos'), 'Especialidad actualizada.');
    }

    public function destroy(Request $r, Especialidad $especialidad)
    {
        $this->exigirAdminGeneral($r);

        if ($especialidad->es_sistema) {
            return $this->failure('No se puede eliminar una especialidad protegida del sistema.', 422);
        }
        if ($especialidad->medicos()->exists() || $especialidad->citas()->exists()) {
            return $this->failure('No se puede eliminar la especialidad porque tiene médicos o citas asociadas.', 422);
        }

        $especialidad->delete();

        return $this->success(null, 'Especialidad eliminada.');
    }

    private function exigirAdminGeneral(Request $r): void
    {
        $u = $r->user();
        $cargo = mb_strtolower((string) ($u->cargo ?? ''));
        $usuario = mb_strtolower((string) ($u->usuario ?? ''));
        if (str_contains($cargo, 'operaciones') || $usuario === 'operaciones') {
            abort(403, 'Solo el Administrador General puede gestionar el catálogo de especialidades.');
        }
    }
}
