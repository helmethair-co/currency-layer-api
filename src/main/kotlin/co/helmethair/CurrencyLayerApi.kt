package co.helmethair

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors


object ParamFields {
    const val ACCESS_KEY = "access_key"
    const val FROM = "from"
    const val TO = "to"
    const val AMOUNT = "amount"
}

object Endpoints {
    const val LIST = "list"
    const val CONVERT = "convert"
}

data class ListResponse(
    val success: Boolean,
    val terms: String,
    val privacy: String,
    val currencies: Map<String, String>
)

data class ConvertResponse(
    val success: Boolean,
    val terms: String,
    val privacy: String,
    val query: ConvertQuery,
    val info: ConvertInfo,
    val result: Double
)

data class ConvertQuery(
    val from: String,
    val to: String,
    val amount: Double
)


data class ConvertInfo(
    val timestamp: Long,
    val quote: Double
)

class CurrencyLayerApi(private val accessKey: String) {
    private val jsonMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    private val cachedThreadPool = Executors.newCachedThreadPool()

    companion object {
        private const val BASE_URL = "http://apilayer.net/api/"
    }

    init {
        FuelManager.instance.basePath = BASE_URL
    }

    fun list(): ListResponse {
        val (request, response, result) = Endpoints.LIST.httpGet(
            parameters = listOf(
                ParamFields.ACCESS_KEY to accessKey
            ))
            .responseString()

        when (result) {
            is Result.Success -> {
                return jsonMapper.readValue(result.value, jacksonTypeRef<ListResponse>())
            }
            is Result.Failure -> {
                throw result.error
            }
        }
    }

    fun listAsync(): CompletableFuture<ListResponse> {
        return calculateAsync(CompletableFuture<ListResponse>()) { future ->
            listAsyncBlock(future)
        }
    }

    fun convert(query: ConvertQuery): String {
        val (request, response, result) = Endpoints.CONVERT.httpGet(
            parameters = listOf(
                ParamFields.ACCESS_KEY to accessKey,
                ParamFields.FROM to query.from,
                ParamFields.TO to query.to,
                ParamFields.AMOUNT to query.amount
            ))
            .responseString()

        when (result) {
            is Result.Success -> {
                return result.value
            }
            is Result.Failure -> {
                throw result.error
            }
        }
    }

    fun convertAsync(query: ConvertQuery): CompletableFuture<ConvertResponse> {
        return calculateAsync(CompletableFuture<ConvertResponse>()) { future ->
            convertAsyncBlock(future, query)
        }
    }

    private fun <T>calculateAsync(future: CompletableFuture<T>, block: (future: CompletableFuture<T>) -> CompletableFuture<T>): CompletableFuture<T> {
        cachedThreadPool.submit { block(future) }
        return future
    }

    private fun listAsyncBlock(future: CompletableFuture<ListResponse>): CompletableFuture<ListResponse> {
        Endpoints.LIST.httpGet(
                parameters = listOf(
                        ParamFields.ACCESS_KEY to accessKey
                ))
                .responseString { request, response, result ->
                    when (result) {
                        is Result.Success -> {
                            val listObject = jsonMapper.readValue<ListResponse>(result.get(), jacksonTypeRef<ListResponse>())
                            future.complete(listObject)
                        }
                        is Result.Failure -> {
                            throw result.error
                        }
                    }
                }
        return future
    }

    private fun convertAsyncBlock(future: CompletableFuture<ConvertResponse>, query: ConvertQuery): CompletableFuture<ConvertResponse> {
        Endpoints.CONVERT.httpGet(
            parameters = listOf(
                ParamFields.ACCESS_KEY to accessKey,
                ParamFields.FROM to query.from,
                ParamFields.TO to query.to,
                ParamFields.AMOUNT to query.amount
            ))
            .responseString { request, response, result ->
                when (result) {
                    is Result.Success -> {
                        val convertObject = jsonMapper.readValue<ConvertResponse>(result.get(), jacksonTypeRef<ConvertResponse>())
                        future.complete(convertObject)
                    }
                    is Result.Failure -> {
                        throw result.error
                    }
                }
            }
        return future
    }
}
