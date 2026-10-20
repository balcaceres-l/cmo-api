<?php

namespace Database\Seeders;

use App\Models\HistorialClinico;
use App\Models\Paciente;
use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class UsuarioDemoSeeder extends Seeder
{
    public function run(): void
    {
        $personal = [['admin', 'Administrador General', 'ADMINISTRADOR', 'Administración'], ['medico.general', 'Dra. Ana Martínez', 'MEDICO', 'Medicina general'], ['medico.emergencia', 'Dr. Carlos López', 'MEDICO', 'Emergencia'], ['medico.alergista', 'Dra. Sofía Rivera', 'MEDICO', 'Alergista'], ['recepcion', 'María Hernández', 'RECEPCIONISTA', 'Recepcionista'], ['enfermeria', 'José Ramírez', 'RECEPCIONISTA', 'Enfermera']];
        foreach ($personal as [$usuario,$nombre,$rol,$cargo]) {
            User::updateOrCreate(['usuario' => $usuario], ['nombre_completo' => $nombre, 'email' => "$usuario@hospital.edu.sv", 'password' => Hash::make('Hospital2026!'), 'rol' => $rol, 'cargo' => $cargo, 'estado' => 'ACTIVO']);
        }foreach ([['paciente.uno', 'Paciente Uno', '01234567-8', 'F'], ['paciente.dos', 'Paciente Dos', '87654321-0', 'M']] as $i => [$usuario,$nombre,$dui,$sexo]) {
            $u = User::updateOrCreate(['usuario' => $usuario], ['nombre_completo' => $nombre, 'email' => "$usuario@hospital.edu.sv", 'password' => Hash::make('Hospital2026!'), 'rol' => 'PACIENTE', 'estado' => 'ACTIVO']);
            $p = Paciente::updateOrCreate(['dui' => $dui], ['id_usuario' => $u->id, 'numero_expediente' => sprintf('EXP-%s-%06d', now()->year, $i + 1), 'nombre_completo' => $nombre, 'fecha_nacimiento' => '1990-01-01', 'sexo' => $sexo, 'fecha_registro' => now()]);
            HistorialClinico::updateOrCreate(['id_paciente' => $p->getKey()], []);
        }
    }
}
