<?php

namespace Tests\Feature\Api;

use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class PrivacidadTest extends TestCase
{
    use RefreshDatabase;

    public function test_medico_no_tratante_y_recepcion_no_ven_expediente(): void
    {
        $p = Paciente::create(['numero_expediente' => 'EXP-2026-000098', 'nombre_completo' => 'Privado', 'fecha_nacimiento' => '1990-01-01', 'sexo' => 'M', 'fecha_registro' => now()]);
        HistorialClinico::create(['id_paciente' => $p->id_paciente, 'antecedentes_personales' => 'Dato clínico']);
        foreach (['MEDICO', 'RECEPCIONISTA'] as $rol) {
            Sanctum::actingAs(User::factory()->create(['rol' => $rol]));
            $this->getJson("/api/v1/pacientes/{$p->id_paciente}/expediente")->assertForbidden();
        }
    }
}
