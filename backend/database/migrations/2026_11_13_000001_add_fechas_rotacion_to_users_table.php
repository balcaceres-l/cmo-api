<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $t) {
            if (! Schema::hasColumn('users', 'fecha_ingreso')) {
                $t->date('fecha_ingreso')->nullable()->after('estado');
            }
            if (! Schema::hasColumn('users', 'fecha_salida')) {
                $t->date('fecha_salida')->nullable()->after('fecha_ingreso');
            }
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $t) {
            $cols = array_filter(['fecha_ingreso', 'fecha_salida'], fn ($c) => Schema::hasColumn('users', $c));
            if ($cols) {
                $t->dropColumn($cols);
            }
        });
    }
};

