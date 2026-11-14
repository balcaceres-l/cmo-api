<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
    use HasApiTokens,HasFactory,HasUuids,Notifiable;

    public const CARGOS_ADMINISTRADOR = ['Administración', 'Jefe de Operaciones'];

    public const CARGOS_RECEPCIONISTA = ['Recepcionista', 'Enfermera'];

    public static function cargosParaRol(string $rol): array
    {
        return match ($rol) {
            'ADMINISTRADOR' => self::CARGOS_ADMINISTRADOR,
            'RECEPCIONISTA' => self::CARGOS_RECEPCIONISTA,
            default => [],
        };
    }

    public $incrementing = false;

    protected $keyType = 'string';

    protected $guarded = [];

    protected $hidden = ['password', 'remember_token'];

    protected $appends = ['especialidad_nombre'];

    protected function casts(): array
    {
        return [
            'password' => 'hashed',
            'fecha_ingreso' => 'date:Y-m-d',
            'fecha_salida' => 'date:Y-m-d',
        ];
    }

    /**
     * Verifica si el colaborador se encuentra activo y dentro de su periodo de
     * contratación/rotación en la fecha indicada (por defecto hoy).
     */
    public function estaVigenteEn(\Carbon\CarbonInterface|string|null $fecha = null): bool
    {
        if ($this->estado !== 'ACTIVO') {
            return false;
        }
        $dia = \Illuminate\Support\Carbon::parse($fecha ?? today())->startOfDay();
        if ($this->fecha_ingreso && $dia->lt(\Illuminate\Support\Carbon::parse($this->fecha_ingreso)->startOfDay())) {
            return false;
        }
        if ($this->fecha_salida && $dia->gt(\Illuminate\Support\Carbon::parse($this->fecha_salida)->startOfDay())) {
            return false;
        }

        return true;
    }

    /**
     * Desactiva automáticamente las cuentas cuyo contrato/rotación ya venció.
     */
    public static function sincronizarVigenciasVencidas(): void
    {
        static::where('estado', 'ACTIVO')
            ->whereNotNull('fecha_salida')
            ->whereDate('fecha_salida', '<', today())
            ->update(['estado' => 'INACTIVO']);
    }

    public function especialidad(): BelongsTo
    {
        return $this->belongsTo(Especialidad::class, 'id_especialidad', 'id_especialidad');
    }

    public function getEspecialidadNombreAttribute(): ?string
    {
        if (($this->attributes['rol'] ?? null) !== 'MEDICO') {
            return null;
        }
        if ($this->relationLoaded('especialidad')) {
            return $this->getRelation('especialidad')?->nombre ?? ($this->attributes['cargo'] ?? null);
        }
        $idEsp = $this->attributes['id_especialidad'] ?? null;

        return $idEsp ? Especialidad::whereKey($idEsp)->value('nombre') : ($this->attributes['cargo'] ?? null);
    }

    public function paciente(): HasOne
    {
        return $this->hasOne(Paciente::class, 'id_usuario');
    }

    public function turnos(): HasMany
    {
        return $this->hasMany(TurnoPersonal::class, 'id_usuario');
    }
}
