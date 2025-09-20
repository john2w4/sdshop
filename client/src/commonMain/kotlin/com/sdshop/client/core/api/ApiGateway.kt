package com.sdshop.client.core.api

import kotlinx.coroutines.delay

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Error(val throwable: Throwable) : ApiResult<Nothing>
}

data class ApiRequest(
    val path: String,
    val payload: Any? = null,
    val method: HttpMethod = HttpMethod.GET
)

enum class HttpMethod { GET, POST, PUT, DELETE }

interface ApiGateway {
    suspend fun <T> execute(request: ApiRequest, handler: suspend (ApiRequest) -> T): ApiResult<T>
}

class LoggingApiGateway : ApiGateway {
    override suspend fun <T> execute(request: ApiRequest, handler: suspend (ApiRequest) -> T): ApiResult<T> =
        try {
            delay(50)
            ApiResult.Success(handler(request))
        } catch (t: Throwable) {
            ApiResult.Error(t)
        }
}
