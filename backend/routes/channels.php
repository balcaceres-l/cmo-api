<?php

use Illuminate\Support\Facades\Broadcast;

// Los ids son UUID: un cast a int los dejaría casi todos en 0 y cualquier
// usuario podría suscribirse al canal privado de otro.
Broadcast::channel('App.Models.User.{id}', function ($user, $id) {
    return (string) $user->id === (string) $id;
});

// Canal del personal del hospital: reportes clínicos de la ambulancia en camino.
// Los pacientes no tienen acceso.
Broadcast::channel('personal', function ($user) {
    return $user->estado === 'ACTIVO' && in_array($user->rol, ['ADMINISTRADOR', 'MEDICO', 'RECEPCIONISTA'], true);
});
