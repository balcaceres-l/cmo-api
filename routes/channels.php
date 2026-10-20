<?php

use Illuminate\Support\Facades\Broadcast;

// Los ids son UUID: un cast a int los dejaría casi todos en 0 y cualquier
// usuario podría suscribirse al canal privado de otro.
Broadcast::channel('App.Models.User.{id}', function ($user, $id) {
    return (string) $user->id === (string) $id;
});
