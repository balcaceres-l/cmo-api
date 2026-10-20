<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Concerns\RespondsWithJson;
use App\Http\Controllers\Controller;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Foundation\Auth\Access\AuthorizesRequests;
use Illuminate\Http\Request;

abstract class ApiController extends Controller
{
    use AuthorizesRequests,RespondsWithJson;

    protected function paginar(Builder $q, Request $r)
    {
        return $q->paginate(min(max((int) $r->input('per_page', 15), 1), 100));
    }
}
