<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('paciente', function (Blueprint $t) {
            $t->uuid('id_paciente')->primary();
            $t->foreignUuid('id_usuario')->nullable()->unique()->constrained('users')->restrictOnDelete();
            $t->string('numero_expediente')->unique();
            $t->string('nombre_completo');
            $t->date('fecha_nacimiento');
            $t->string('dui', 12)->nullable()->unique();
            $t->enum('sexo', ['M', 'F']);
            $t->string('telefono', 25)->nullable();
            $t->text('direccion')->nullable();
            $t->string('tipo_sangre', 5)->nullable();
            $t->text('alergias')->nullable();
            $t->foreignUuid('id_registrado_por')->nullable()->constrained('users')->restrictOnDelete();
            $t->timestamp('fecha_registro')->useCurrent();
        });
        Schema::create('historial_clinico', function (Blueprint $t) {
            $t->uuid('id_historial')->primary();
            $t->foreignUuid('id_paciente')->unique()->constrained('paciente', 'id_paciente')->cascadeOnDelete();
            $t->text('antecedentes_personales')->nullable();
            $t->text('antecedentes_familiares')->nullable();
            $t->timestamps();
        });
        Schema::create('turno_personal', function (Blueprint $t) {
            $t->uuid('id_turno')->primary();
            $t->foreignUuid('id_usuario')->constrained('users')->restrictOnDelete();
            $t->date('fecha');
            $t->time('hora_inicio');
            $t->time('hora_fin');
            $t->string('area');
            $t->timestamps();
            $t->index(['id_usuario', 'fecha']);
        });
        Schema::create('cita', function (Blueprint $t) {
            $t->uuid('id_cita')->primary();
            $t->foreignUuid('id_paciente')->constrained('paciente', 'id_paciente')->restrictOnDelete();
            $t->foreignUuid('id_medico')->nullable()->constrained('users')->restrictOnDelete();
            $t->date('fecha')->nullable();
            $t->time('hora_inicio')->nullable();
            $t->time('hora_fin')->nullable();
            $t->text('motivo');
            $t->enum('estado', ['SOLICITADA', 'AGENDADA', 'EN_ESPERA', 'EN_ATENCION', 'ATENDIDA', 'CANCELADA', 'NO_ASISTIO'])->default('AGENDADA');
            $t->text('motivo_cancelacion')->nullable();
            $t->timestamp('hora_llegada')->nullable();
            $t->foreignUuid('id_creado_por')->constrained('users')->restrictOnDelete();
            $t->timestamps();
            $t->index(['id_medico', 'fecha']);
            $t->index(['fecha', 'estado']);
        });
        Schema::create('consulta', function (Blueprint $t) {
            $t->uuid('id_consulta')->primary();
            $t->foreignUuid('id_cita')->unique()->constrained('cita', 'id_cita')->restrictOnDelete();
            $t->foreignUuid('id_medico')->constrained('users')->restrictOnDelete();
            $t->timestamp('fecha_hora_inicio');
            $t->timestamp('fecha_hora_fin')->nullable();
            $t->text('motivo')->nullable();
            $t->text('diagnostico')->nullable();
            $t->text('notas')->nullable();
            $t->timestamps();
        });
        Schema::create('receta', function (Blueprint $t) {
            $t->uuid('id_receta')->primary();
            $t->foreignUuid('id_consulta')->constrained('consulta', 'id_consulta')->cascadeOnDelete();
            $t->string('medicamento');
            $t->string('dosis');
            $t->string('frecuencia');
            $t->string('duracion');
            $t->text('indicaciones')->nullable();
            $t->timestamps();
        });
        Schema::create('examen_catalogo', function (Blueprint $t) {
            $t->uuid('id_examen')->primary();
            $t->string('nombre')->unique();
            $t->text('descripcion')->nullable();
            $t->boolean('activo')->default(true);
        });
        Schema::create('orden_laboratorio', function (Blueprint $t) {
            $t->uuid('id_orden')->primary();
            $t->foreignUuid('id_consulta')->constrained('consulta', 'id_consulta')->restrictOnDelete();
            $t->foreignUuid('id_paciente')->constrained('paciente', 'id_paciente')->restrictOnDelete();
            $t->foreignUuid('id_examen')->constrained('examen_catalogo', 'id_examen')->restrictOnDelete();
            $t->foreignUuid('id_medico')->constrained('users')->restrictOnDelete();
            $t->enum('estado', ['PENDIENTE', 'EN_PROCESO', 'COMPLETADO'])->default('PENDIENTE');
            $t->text('resultado')->nullable();
            $t->text('observaciones')->nullable();
            $t->foreignUuid('id_registrado_por')->nullable()->constrained('users')->restrictOnDelete();
            $t->timestamp('fecha_solicitud')->useCurrent();
            $t->timestamp('fecha_inicio')->nullable();
            $t->timestamp('fecha_resultado')->nullable();
            $t->timestamps();
            $t->index(['estado', 'fecha_solicitud']);
        });
        Schema::create('habitacion', function (Blueprint $t) {
            $t->uuid('id_habitacion')->primary();
            $t->string('codigo')->unique();
            $t->unsignedTinyInteger('planta');
            $t->enum('tipo', ['VIP', 'SEMIPRIVADA']);
            $t->unsignedTinyInteger('capacidad');
        });
        Schema::create('cama', function (Blueprint $t) {
            $t->uuid('id_cama')->primary();
            $t->foreignUuid('id_habitacion')->constrained('habitacion', 'id_habitacion')->restrictOnDelete();
            $t->string('codigo')->unique();
            $t->enum('estado', ['DISPONIBLE', 'OCUPADA'])->default('DISPONIBLE');
        });
        Schema::create('ingreso', function (Blueprint $t) {
            $t->uuid('id_ingreso')->primary();
            $t->foreignUuid('id_paciente')->constrained('paciente', 'id_paciente')->restrictOnDelete();
            $t->foreignUuid('id_cama')->constrained('cama', 'id_cama')->restrictOnDelete();
            $t->foreignUuid('id_medico')->constrained('users')->restrictOnDelete();
            $t->timestamp('fecha_ingreso');
            $t->timestamp('fecha_alta')->nullable();
            $t->text('motivo');
            $t->foreignUuid('id_registrado_por')->constrained('users')->restrictOnDelete();
            $t->timestamps();
            // Con DB_CONNECTION=mariadb el driver se llama "mariadb", no "mysql";
            // sin esta condición la base quedaría sin la protección de cupo.
            if (in_array(DB::getDriverName(), ['mysql', 'mariadb'], true)) {
                $t->uuid('cama_activa')->nullable()->storedAs('IF(fecha_alta IS NULL, id_cama, NULL)')->unique();
                $t->uuid('paciente_activo')->nullable()->storedAs('IF(fecha_alta IS NULL, id_paciente, NULL)')->unique();
            }
        });
        if (DB::getDriverName() === 'sqlite') {
            DB::statement('CREATE UNIQUE INDEX ingreso_cama_activa_unique ON ingreso (id_cama) WHERE fecha_alta IS NULL');
            DB::statement('CREATE UNIQUE INDEX ingreso_paciente_activo_unique ON ingreso (id_paciente) WHERE fecha_alta IS NULL');
        }
        Schema::create('recurso', function (Blueprint $t) {
            $t->uuid('id_recurso')->primary();
            $t->string('nombre');
            $t->string('categoria');
            $t->string('unidad');
            $t->unsignedInteger('cantidad')->default(0);
            $t->unsignedInteger('stock_minimo')->default(0);
            $t->string('ubicacion')->nullable();
            $t->timestamps();
        });
        Schema::create('movimiento_inventario', function (Blueprint $t) {
            $t->uuid('id_movimiento')->primary();
            $t->foreignUuid('id_recurso')->constrained('recurso', 'id_recurso')->restrictOnDelete();
            $t->foreignUuid('id_usuario')->constrained('users')->restrictOnDelete();
            $t->enum('tipo', ['ENTRADA', 'SALIDA']);
            $t->unsignedInteger('cantidad');
            $t->text('motivo');
            $t->timestamp('fecha');
        });
        Schema::create('ambulancia', function (Blueprint $t) {
            $t->uuid('id_ambulancia')->primary();
            $t->string('placa')->unique();
            $t->enum('estado', ['DISPONIBLE', 'EN_SERVICIO', 'EN_MANTENIMIENTO'])->default('DISPONIBLE');
            $t->timestamps();
        });
        Schema::create('servicio_ambulancia', function (Blueprint $t) {
            $t->uuid('id_servicio')->primary();
            $t->foreignUuid('id_ambulancia')->constrained('ambulancia', 'id_ambulancia')->restrictOnDelete();
            $t->foreignUuid('id_solicitante')->constrained('users')->restrictOnDelete();
            $t->foreignUuid('id_paciente')->nullable()->constrained('paciente', 'id_paciente')->restrictOnDelete();
            $t->string('paciente_nombre')->nullable();
            $t->text('direccion');
            $t->text('descripcion');
            $t->enum('prioridad', ['BAJA', 'MEDIA', 'ALTA', 'CRITICA']);
            $t->enum('estado', ['EN_CURSO', 'FINALIZADO', 'CANCELADO'])->default('EN_CURSO');
            $t->timestamp('hora_solicitud');
            $t->timestamp('hora_salida')->nullable();
            $t->timestamp('hora_cierre')->nullable();
            $t->text('notas')->nullable();
            $t->timestamps();
            $t->index('estado');
            $t->index('hora_solicitud');
        });
        Schema::create('aviso', function (Blueprint $t) {
            $t->uuid('id_aviso')->primary();
            $t->foreignUuid('id_autor')->constrained('users')->restrictOnDelete();
            $t->string('titulo');
            $t->text('contenido');
            $t->boolean('publico')->default(false);
            $t->timestamp('vigente_desde');
            $t->timestamp('vigente_hasta')->nullable();
            $t->timestamps();
        });
    }

    public function down(): void
    {
        foreach (['aviso', 'servicio_ambulancia', 'ambulancia', 'movimiento_inventario', 'recurso', 'ingreso', 'cama', 'habitacion', 'orden_laboratorio', 'examen_catalogo', 'receta', 'consulta', 'cita', 'turno_personal', 'historial_clinico', 'paciente'] as $table) {
            Schema::dropIfExists($table);
        }
    }
};
