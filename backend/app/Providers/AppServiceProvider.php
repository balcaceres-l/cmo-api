<?php

namespace App\Providers;

use App\Models\Consulta;
use App\Models\Ingreso;
use App\Models\OrdenLaboratorio;
use App\Models\Paciente;
use App\Policies\ConsultaPolicy;
use App\Policies\IngresoPolicy;
use App\Policies\OrdenLaboratorioPolicy;
use App\Policies\PacientePolicy;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\Facades\URL;
use Illuminate\Support\ServiceProvider;
use Illuminate\Validation\Rules\Password;

class AppServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        //
    }

    public function boot(): void
    {
        Gate::policy(Paciente::class, PacientePolicy::class);
        Gate::policy(Consulta::class, ConsultaPolicy::class);
        Gate::policy(OrdenLaboratorio::class, OrdenLaboratorioPolicy::class);
        Gate::policy(Ingreso::class, IngresoPolicy::class);
        // uncompromised() consulta HaveIBeenPwned, por eso solo en producción.
        Password::defaults(fn () => app()->isProduction()
            ? Password::min(10)->letters()->mixedCase()->numbers()->symbols()->uncompromised()
            : Password::min(8)->letters()->numbers());

        Model::preventLazyLoading(! app()->isProduction());
        Model::preventSilentlyDiscardingAttributes(! app()->isProduction());

        if (app()->isProduction()) {
            URL::forceScheme('https');
        }

        // Se limita por usuario e IP a la vez para que varios intentos desde
        // una red compartida no bloqueen al resto del hospital.
        RateLimiter::for('login', fn (Request $request) => [
            Limit::perMinute(5)->by($request->input('usuario').'|'.$request->ip()),
            Limit::perMinute(20)->by($request->ip()),
        ]);

        RateLimiter::for('api', fn (Request $request) => Limit::perMinute(120)
            ->by($request->user()?->id ?: $request->ip()));

        RateLimiter::for('registro', fn (Request $request) => Limit::perMinute(6)->by($request->ip()));
    }
}
