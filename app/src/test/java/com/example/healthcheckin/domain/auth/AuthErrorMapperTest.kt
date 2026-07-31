package com.example.healthcheckin.domain.auth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthErrorMapperTest {

    @Test
    fun mapsUserAlreadyExists() {
        val exception = httpException(
            code = 422,
            body = """{"error_code":"user_already_exists","msg":"User already registered"}""",
        )

        assertEquals(AuthException.EmailAlreadyRegistered, AuthErrorMapper.fromHttpException(exception))
    }

    @Test
    fun mapsInvalidGrant() {
        val exception = httpException(
            code = 400,
            body = """{"error":"invalid_grant","error_description":"Invalid login credentials"}""",
        )

        assertEquals(AuthException.InvalidCredentials, AuthErrorMapper.fromHttpException(exception))
    }

    @Test
    fun mapsNetworkErrors() {
        assertTrue(AuthErrorMapper.fromThrowable(UnknownHostException()) is AuthException.NetworkUnavailable)
        assertTrue(AuthErrorMapper.fromThrowable(SocketTimeoutException()) is AuthException.Timeout)
    }

    private fun httpException(code: Int, body: String): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(code, responseBody))
    }
}
