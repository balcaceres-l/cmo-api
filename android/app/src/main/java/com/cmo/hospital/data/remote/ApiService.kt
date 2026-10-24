package com.cmo.hospital.data.remote

import com.cmo.hospital.data.model.ApiResponse
import com.cmo.hospital.data.model.AuthResponseData
import com.cmo.hospital.data.model.Cita
import com.cmo.hospital.data.model.LoginRequest
import com.cmo.hospital.data.model.PaginatedData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponseData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("citas")
    suspend fun getCitas(): Response<ApiResponse<PaginatedData<Cita>>>

    @GET("publico/ambulancia")
    suspend fun getAmbulancia(): Response<ApiResponse<Map<String, Any>>>
}
