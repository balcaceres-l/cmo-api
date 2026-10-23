<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Reportes que el personal de la ambulancia envía durante el traslado, para
 * que emergencia prepare la atención antes de que llegue el paciente.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('reporte_ambulancia', function (Blueprint $t) {
            $t->uuid('id_reporte')->primary();
            $t->foreignUuid('id_servicio')->constrained('servicio_ambulancia', 'id_servicio')->restrictOnDelete();
            $t->foreignUuid('id_autor')->constrained('users')->restrictOnDelete();
            $t->enum('estado_paciente', ['ESTABLE', 'DELICADO', 'CRITICO']);
            $t->text('observaciones')->nullable();
            $t->timestamp('llegada_estimada')->nullable();
            $t->timestamp('fecha')->useCurrent();
            $t->index(['id_servicio', 'fecha']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('reporte_ambulancia');
    }
};
