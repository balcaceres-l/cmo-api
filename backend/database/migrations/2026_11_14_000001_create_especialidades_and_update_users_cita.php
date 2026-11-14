<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Str;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('especialidad', function (Blueprint $t) {
            $t->uuid('id_especialidad')->primary();
            $t->string('nombre', 100)->unique();
            $t->string('descripcion', 255)->nullable();
            $t->boolean('es_sistema')->default(false);
            $t->boolean('activo')->default(true);
            $t->timestamps();
        });

        $ahora = now();
        $base = [
            ['Emergencia', 'Atención médica de urgencias y guardia 24/7', true],
            ['Medicina general', 'Consulta externa general y atención primaria', true],
            ['Alergista', 'Alergología e inmunología clínica', false],
            ['Pediatría', 'Atención médica infantil y del adolescente', false],
            ['Cardiología', 'Diagnóstico y tratamiento cardiovascular', false],
            ['Ginecología', 'Salud integral femenina y obstetricia', false],
        ];

        $idsPorNombre = [];
        foreach ($base as [$nombre, $descripcion, $esSistema]) {
            $id = (string) Str::uuid();
            $idsPorNombre[mb_strtolower($nombre)] = $id;
            DB::table('especialidad')->insert([
                'id_especialidad' => $id,
                'nombre' => $nombre,
                'descripcion' => $descripcion,
                'es_sistema' => $esSistema,
                'activo' => true,
                'created_at' => $ahora,
                'updated_at' => $ahora,
            ]);
        }

        Schema::table('users', function (Blueprint $t) {
            $t->foreignUuid('id_especialidad')
                ->nullable()
                ->after('cargo')
                ->constrained('especialidad', 'id_especialidad')
                ->nullOnDelete();
        });

        // Migrar los médicos existentes que tenían su especialidad guardada en `cargo`.
        $medicos = DB::table('users')->where('rol', 'MEDICO')->get(['id', 'cargo']);
        foreach ($medicos as $medico) {
            $nombreCargo = trim((string) ($medico->cargo ?? ''));
            if ($nombreCargo === '') {
                $idEsp = $idsPorNombre['medicina general'];
            } else {
                $clave = mb_strtolower($nombreCargo);
                if (! isset($idsPorNombre[$clave])) {
                    $idNuevo = (string) Str::uuid();
                    $idsPorNombre[$clave] = $idNuevo;
                    DB::table('especialidad')->insert([
                        'id_especialidad' => $idNuevo,
                        'nombre' => $nombreCargo,
                        'descripcion' => null,
                        'es_sistema' => false,
                        'activo' => true,
                        'created_at' => $ahora,
                        'updated_at' => $ahora,
                    ]);
                }
                $idEsp = $idsPorNombre[$clave];
            }

            DB::table('users')
                ->where('id', $medico->id)
                ->update([
                    'id_especialidad' => $idEsp,
                    'cargo' => null,
                ]);
        }

        Schema::table('cita', function (Blueprint $t) {
            $t->foreignUuid('id_especialidad')
                ->nullable()
                ->after('id_medico')
                ->constrained('especialidad', 'id_especialidad')
                ->nullOnDelete();
            $t->index(['id_especialidad', 'fecha']);
        });

        // Asociar especialidad a citas existentes basadas en su médico asignado.
        $citas = DB::table('cita')->whereNotNull('id_medico')->get(['id_cita', 'id_medico']);
        if ($citas->isNotEmpty()) {
            $medicosEsp = DB::table('users')->whereIn('id', $citas->pluck('id_medico')->unique())->pluck('id_especialidad', 'id');
            foreach ($citas as $cita) {
                $idEsp = $medicosEsp[$cita->id_medico] ?? null;
                if ($idEsp) {
                    DB::table('cita')->where('id_cita', $cita->id_cita)->update(['id_especialidad' => $idEsp]);
                }
            }
        }
    }

    public function down(): void
    {
        Schema::table('cita', function (Blueprint $t) {
            $t->dropIndex(['id_especialidad', 'fecha']);
            $t->dropConstrainedForeignId('id_especialidad');
        });

        $medicos = DB::table('users')->where('rol', 'MEDICO')->whereNotNull('id_especialidad')->get(['id', 'id_especialidad']);
        $nombres = DB::table('especialidad')->pluck('nombre', 'id_especialidad');
        foreach ($medicos as $medico) {
            DB::table('users')->where('id', $medico->id)->update([
                'cargo' => $nombres[$medico->id_especialidad] ?? null,
            ]);
        }

        Schema::table('users', function (Blueprint $t) {
            $t->dropConstrainedForeignId('id_especialidad');
        });

        Schema::dropIfExists('especialidad');
    }
};
