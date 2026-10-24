<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Schema;
use Tests\TestCase;

class SessionsTableTest extends TestCase
{
    use RefreshDatabase;

    public function test_sessions_and_password_reset_tables_exist(): void
    {
        $this->assertTrue(Schema::hasTable('sessions'));
        $this->assertTrue(Schema::hasTable('password_reset_tokens'));
    }

    public function test_database_session_driver_works(): void
    {
        config(['session.driver' => 'database']);

        $response = $this->get('/');
        $response->assertStatus(200);
    }
}
