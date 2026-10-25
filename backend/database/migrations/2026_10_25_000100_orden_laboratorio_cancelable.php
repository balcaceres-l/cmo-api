<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

/**
 * El médico puede cancelar una orden de laboratorio que pidió por error,
 * siempre que el laboratorio todavía no la haya iniciado (`PENDIENTE`).
 * La orden no se borra: queda `CANCELADA` con su fecha y, si se indicó, el motivo.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('orden_laboratorio', function (Blueprint $t) {
            $t->enum('estado', ['PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'CANCELADA'])->default('PENDIENTE')->change();
            $t->text('motivo_cancelacion')->nullable()->after('observaciones');
            $t->timestamp('fecha_cancelacion')->nullable()->after('fecha_resultado');
        });
    }

    public function down(): void
    {
        if (DB::table('orden_laboratorio')->where('estado', 'CANCELADA')->exists()) {
            throw new RuntimeException('Hay órdenes de laboratorio canceladas; no se puede revertir sin perder ese estado.');
        }

        Schema::table('orden_laboratorio', function (Blueprint $t) {
            $t->dropColumn(['motivo_cancelacion', 'fecha_cancelacion']);
            $t->enum('estado', ['PENDIENTE', 'EN_PROCESO', 'COMPLETADO'])->default('PENDIENTE')->change();
        });
    }
};
