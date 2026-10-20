<?php

namespace Tests\Feature\Api;

use Database\Seeders\DatabaseSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PublicoTest extends TestCase
{
    use RefreshDatabase;

    public function test_endpoints_publicos_no_exponen_pacientes(): void
    {
        $this->seed(DatabaseSeeder::class);
        foreach (['ocupacion', 'medicos-de-turno', 'avisos', 'ambulancia'] as $ruta) {
            $texto = $this->getJson("/api/v1/publico/$ruta")->assertOk()->getContent();
            $this->assertStringNotContainsString('nombre_completo', $texto);
            $this->assertStringNotContainsString('id_paciente', $texto);
        }
    }
}
