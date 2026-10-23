<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * El módulo de laboratorio solo informa el estado de cada orden
 * (Pendiente, En proceso, Completado); los datos del examen no se guardan
 * en el sistema.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('orden_laboratorio', function (Blueprint $t) {
            $t->dropColumn('resultado');
        });
    }

    public function down(): void
    {
        Schema::table('orden_laboratorio', function (Blueprint $t) {
            $t->text('resultado')->nullable()->after('estado');
        });
    }
};
