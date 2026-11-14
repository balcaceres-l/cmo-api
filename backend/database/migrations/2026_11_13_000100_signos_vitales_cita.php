<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Signos vitales tomados el día de la cita (triaje en recepción o por el médico
 * en la consulta). Uno por cita: al volver a tomarlos se actualizan.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('signo_vital', function (Blueprint $t) {
            $t->uuid('id_signo')->primary();
            $t->foreignUuid('id_cita')->unique()->constrained('cita', 'id_cita')->cascadeOnDelete();
            $t->unsignedSmallInteger('presion_sistolica')->nullable();
            $t->unsignedSmallInteger('presion_diastolica')->nullable();
            $t->unsignedSmallInteger('frecuencia_cardiaca')->nullable();
            $t->unsignedSmallInteger('frecuencia_respiratoria')->nullable();
            $t->decimal('temperatura_c', 4, 1)->nullable();
            $t->unsignedTinyInteger('saturacion_oxigeno')->nullable();
            $t->decimal('peso_kg', 5, 1)->nullable();
            $t->decimal('talla_cm', 5, 1)->nullable();
            $t->text('observaciones')->nullable();
            $t->foreignUuid('id_registrado_por')->constrained('users')->restrictOnDelete();
            $t->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('signo_vital');
    }
};
