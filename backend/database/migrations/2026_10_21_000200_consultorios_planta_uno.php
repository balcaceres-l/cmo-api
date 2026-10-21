<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Planta 1 del hospital: consultorios de consulta general. Cada cita indica
 * en qué consultorio se atiende; las plantas 2 y 3 son de hospitalización.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('consultorio', function (Blueprint $t) {
            $t->uuid('id_consultorio')->primary();
            $t->string('codigo')->unique();
            $t->unsignedTinyInteger('planta')->default(1);
            $t->string('nombre');
            $t->boolean('activo')->default(true);
        });

        Schema::table('cita', function (Blueprint $t) {
            $t->foreignUuid('id_consultorio')->nullable()->after('id_medico')
                ->constrained('consultorio', 'id_consultorio')->restrictOnDelete();
            $t->index(['id_consultorio', 'fecha']);
        });
    }

    public function down(): void
    {
        Schema::table('cita', function (Blueprint $t) {
            $t->dropIndex(['id_consultorio', 'fecha']);
            $t->dropConstrainedForeignId('id_consultorio');
        });
        Schema::dropIfExists('consultorio');
    }
};
