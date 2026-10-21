# Hospital Universitario API

API REST única para las aplicaciones iOS, Android y el panel administrativo del Hospital Universitario. Está construida con Laravel 12, Sanctum, MariaDB, Reverb y Scramble.

## Requisitos

- PHP 8.2 o superior
- Composer 2
- MariaDB 10.11 o superior

## Instalación

```bash
composer install
cp .env.example .env
php artisan key:generate
php artisan migrate:fresh --seed
```

Configure `APP_URL`, las variables `DB_*` y `REVERB_*` en `.env`. Para iniciar la API y el servidor de eventos:

```bash
php artisan serve
php artisan reverb:start
```

La API usa el prefijo `/api/v1`. La referencia completa (roles, endpoints, validaciones, estados y tiempo real) está en [`docs/API.md`](docs/API.md), y la documentación OpenAPI interactiva en `/docs/api`.

## Usuarios de demostración

Todos usan la contraseña `Hospital2026!`:

| Rol | Usuario |
| --- | --- |
| Administrador | `admin` |
| Médico general | `medico.general` |
| Médico de emergencia | `medico.emergencia` |
| Médico alergista | `medico.alergista` |
| Recepción | `recepcion` |
| Enfermería | `enfermeria` |
| Pacientes | `paciente.uno`, `paciente.dos` |

Los correos de demostración usan el dominio `@hospital.edu.sv`.

## Módulos

Autenticación y registro de pacientes, usuarios y turnos, pacientes y expediente, citas, consultas y recetas, laboratorio, habitaciones e ingresos, inventario, ambulancia en tiempo real, avisos, información pública y métricas administrativas.

## Verificación

```bash
php artisan test
```
