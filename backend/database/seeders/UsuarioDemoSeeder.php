<?php

namespace Database\Seeders;

use App\Models\Especialidad;
use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class UsuarioDemoSeeder extends Seeder
{
    public function run(): void
    {
        $password = Hash::make('Hospital2026!');

        $personal = [
            // Administración y Operaciones (Planta Indefinida)
            ['admin', 'Administrador General', 'ADMINISTRADOR', 'Administración', '2025-01-10', null],
            ['operaciones', 'Lic. Carolina Valenzuela', 'ADMINISTRADOR', 'Jefe de Operaciones', '2025-02-01', null],

            // Médicos de Emergencias (5 para cubrir 24/7 en turnos de 8h; mezcla de planta y residentes rotantes)
            ['medico.emergencia', 'Dr. Carlos López', 'MEDICO', 'Emergencia', '2025-01-15', null],
            ['medico.emergencia2', 'Dr. Roberto Méndez', 'MEDICO', 'Emergencia', '2026-01-15', '2027-01-15'], // 12 meses (Año Académico)
            ['medico.emergencia3', 'Dra. Valeria Castillo', 'MEDICO', 'Emergencia', '2026-07-01', '2026-12-31'], // 6 meses (Semestre)
            ['medico.emergencia4', 'Dr. Andrés Morales', 'MEDICO', 'Emergencia', '2026-07-01', '2026-12-31'], // 6 meses (Semestre)
            ['medico.emergencia5', 'Dra. Camila Serrano', 'MEDICO', 'Emergencia', '2026-09-18', '2026-11-18'], // 2 meses (Rotación corta que finaliza el Mié 18 Nov)

            // Médicos de Consulta Externa y Especialidades
            ['medico.general', 'Dra. Ana Martínez', 'MEDICO', 'Medicina general', '2025-03-01', null],
            ['medico.general2', 'Dr. Diego Navarro', 'MEDICO', 'Medicina general', '2026-09-18', '2026-11-18'], // 2 meses (Rotación corta que finaliza el Mié 18 Nov)
            ['medico.general3', 'Dra. Paola Mendoza', 'MEDICO', 'Medicina general', '2026-08-01', '2027-01-31'], // 6 meses

            ['medico.alergista', 'Dra. Sofía Rivera', 'MEDICO', 'Alergista', '2025-04-01', null],
            ['medico.alergista2', 'Dr. Fernando Vega', 'MEDICO', 'Alergista', '2026-06-01', '2026-11-30'], // 6 meses

            ['medico.pediatria', 'Dra. Lucía Herrera', 'MEDICO', 'Pediatría', '2025-05-01', null],
            ['medico.pediatria2', 'Dr. Gabriel Ortiz', 'MEDICO', 'Pediatría', '2026-02-01', '2027-01-31'], // 12 meses

            ['medico.cardiologia', 'Dr. Alejandro Cruz', 'MEDICO', 'Cardiología', '2025-01-20', null],
            ['medico.cardiologia2', 'Dra. Mónica Paredes', 'MEDICO', 'Cardiología', '2026-07-15', '2027-01-15'], // 6 meses

            ['medico.ginecologia', 'Dra. Carmen Flores', 'MEDICO', 'Ginecología', '2025-06-01', null],
            ['medico.ginecologia2', 'Dra. Gabriela Campos', 'MEDICO', 'Ginecología', '2026-10-01', '2026-11-30'], // 2 meses

            // Enfermería (5 para cubrir Triaje y Enfermería 24/7 en turnos de 8h)
            ['enfermeria', 'José Ramírez', 'RECEPCIONISTA', 'Enfermera', '2025-01-15', null],
            ['enfermeria2', 'Lic. Elena Vargas', 'RECEPCIONISTA', 'Enfermera', '2025-03-10', null],
            ['enfermeria3', 'Lic. Marcos Silva', 'RECEPCIONISTA', 'Enfermera', '2026-01-15', '2027-01-15'], // 12 meses
            ['enfermeria4', 'Lic. Daniela Reyes', 'RECEPCIONISTA', 'Enfermera', '2026-07-01', '2026-12-31'], // 6 meses
            ['enfermeria5', 'Lic. Sofía Molina', 'RECEPCIONISTA', 'Enfermera', '2026-10-01', '2026-11-30'], // 2 meses

            // Recepción (5 para cubrir Admisión y Recepción 24/7 en turnos de 8h)
            ['recepcion', 'María Hernández', 'RECEPCIONISTA', 'Recepcionista', '2025-01-15', null],
            ['recepcion2', 'Laura Gómez', 'RECEPCIONISTA', 'Recepcionista', '2025-04-01', null],
            ['recepcion3', 'Kevin Vásquez', 'RECEPCIONISTA', 'Recepcionista', '2026-01-15', '2027-01-15'], // 12 meses
            ['recepcion4', 'Patricia Pineda', 'RECEPCIONISTA', 'Recepcionista', '2026-07-01', '2026-12-31'], // 6 meses
            ['recepcion5', 'Esteban Aguilar', 'RECEPCIONISTA', 'Recepcionista', '2026-07-01', '2026-12-31'], // 6 meses
        ];

        $catalogoEspecialidades = [
            ['Emergencia', 'Atención médica de urgencias y guardia 24/7', true],
            ['Medicina general', 'Consulta externa general y atención primaria', true],
            ['Alergista', 'Alergología e inmunología clínica', false],
            ['Pediatría', 'Atención médica infantil y del adolescente', false],
            ['Cardiología', 'Diagnóstico y tratamiento cardiovascular', false],
            ['Ginecología', 'Salud integral femenina y obstetricia', false],
        ];

        $especialidadesPorNombre = [];
        foreach ($catalogoEspecialidades as [$espNombre, $espDesc, $esSistema]) {
            $esp = Especialidad::updateOrCreate(
                ['nombre' => $espNombre],
                ['descripcion' => $espDesc, 'es_sistema' => $esSistema, 'activo' => true]
            );
            $especialidadesPorNombre[mb_strtolower($espNombre)] = $esp->id_especialidad;
        }

        foreach ($personal as [$usuario, $nombre, $rol, $cargo, $fechaIngreso, $fechaSalida]) {
            $esMedico = $rol === 'MEDICO';
            $idEsp = $esMedico ? ($especialidadesPorNombre[mb_strtolower($cargo)] ?? null) : null;

            User::updateOrCreate(
                ['usuario' => $usuario],
                [
                    'nombre_completo' => $nombre,
                    'email' => "$usuario@hospital.edu.sv",
                    'password' => $password,
                    'rol' => $rol,
                    'cargo' => $esMedico ? null : $cargo,
                    'id_especialidad' => $idEsp,
                    'estado' => 'ACTIVO',
                    'fecha_ingreso' => $fechaIngreso,
                    'fecha_salida' => $fechaSalida,
                ]
            );
        }

        $pacientes = [
            ['paciente.uno', 'Paciente Uno', '01234567-8', 'F', '1990-01-01', 'O+', 'Ninguna conocida', '7123-4501'],
            ['paciente.dos', 'Paciente Dos', '87654321-0', 'M', '1988-05-14', 'A+', 'Penicilina', '7123-4502'],
            ['paciente.03', 'Juan Carlos Alvarado', '03456789-1', 'M', '1979-03-22', 'B+', 'Ninguna conocida', '7234-1103'],
            ['paciente.04', 'María José Quintanilla', '04567890-2', 'F', '1995-11-08', 'O-', 'Sulfas', '7234-1104'],
            ['paciente.05', 'Luis Fernando Ayala', '05678901-3', 'M', '1982-07-19', 'A-', 'Ibuprofeno', '7234-1105'],
            ['paciente.06', 'Andrea Beatriz Portillo', '06789012-4', 'F', '2001-02-11', 'O+', 'Polvo y ácaros', '7234-1106'],
            ['paciente.07', 'Carlos Eduardo Mejía', '07890123-5', 'M', '1965-09-30', 'AB+', 'Ninguna conocida', '7234-1107'],
            ['paciente.08', 'Rosa Emilia Benítez', '08901234-6', 'F', '1973-12-04', 'A+', 'Aspirina', '7234-1108'],
            ['paciente.09', 'Mateo Sebastián Guzmán', '09012345-7', 'M', '2016-06-15', 'O+', 'Mariscos', '7234-1109'],
            ['paciente.10', 'Sofía Alejandra Escobar', '01122334-8', 'F', '1998-04-27', 'B-', 'Ninguna conocida', '7234-1110'],
            ['paciente.11', 'Héctor Mauricio Lemus', '02233445-9', 'M', '1958-08-12', 'O+', 'Dipirona', '7234-1111'],
            ['paciente.12', 'Claudia Marisol Renderos', '03344556-0', 'F', '1986-10-03', 'A+', 'Ninguna conocida', '7234-1112'],
            ['paciente.13', 'Óscar René Zelaya', '04455667-1', 'M', '1992-01-25', 'O+', 'Látex', '7234-1113'],
            ['paciente.14', 'Karla Lisseth Bonilla', '05566778-2', 'F', '1984-05-09', 'B+', 'Ninguna conocida', '7234-1114'],
            ['paciente.15', 'Daniel Alejandro Rivas', '06677889-3', 'M', '2004-07-17', 'O+', 'Ninguna conocida', '7234-1115'],
        ];

        foreach ($pacientes as $i => [$usuario, $nombre, $dui, $sexo, $nacimiento, $sangre, $alergias, $tel]) {
            $u = User::updateOrCreate(
                ['usuario' => $usuario],
                [
                    'nombre_completo' => $nombre,
                    'email' => "$usuario@hospital.edu.sv",
                    'password' => $password,
                    'rol' => 'PACIENTE',
                    'estado' => 'ACTIVO',
                ]
            );

            $p = Paciente::updateOrCreate(
                ['dui' => $dui],
                [
                    'id_usuario' => $u->id,
                    'numero_expediente' => sprintf('EXP-%s-%06d', now()->year, $i + 1),
                    'nombre_completo' => $nombre,
                    'fecha_nacimiento' => $nacimiento,
                    'sexo' => $sexo,
                    'telefono' => $tel,
                    'tipo_sangre' => $sangre,
                    'alergias' => $alergias,
                    'fecha_registro' => now(),
                ]
            );

            HistorialClinico::updateOrCreate(['id_paciente' => $p->getKey()], []);
        }
    }
}
