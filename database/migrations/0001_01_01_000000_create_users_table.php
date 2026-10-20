<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('users', function (Blueprint $t) {
            $t->uuid('id')->primary();
            $t->string('nombre_completo');
            $t->string('usuario')->unique();
            $t->string('email')->nullable()->unique();
            $t->string('password');
            $t->enum('rol', ['ADMINISTRADOR', 'MEDICO', 'RECEPCIONISTA', 'PACIENTE']);
            $t->string('cargo')->nullable();
            $t->string('telefono', 25)->nullable();
            $t->enum('estado', ['ACTIVO', 'INACTIVO'])->default('ACTIVO');
            $t->rememberToken();
            $t->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('users');
    }
};
