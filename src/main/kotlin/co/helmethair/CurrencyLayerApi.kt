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
    const val CURRENCIES = "currencies"
    const val SOURCE = "source"
}

object Endpoints {
    const val LIST = "list"
    const val LIVE = "live"
    const val CONVERT = "convert"
}

interface Response {
    val success: Boolean
    val terms: String
    val privacy: String
}

data class ListResponse(
    override val success: Boolean,
    override val terms: String,
    override val privacy: String,
    val currencies: Map<String, String>
): Response

data class ConvertResponse(
    override val success: Boolean,
    override val terms: String,
    override val privacy: String,
    val query: ConvertQuery,
    val info: ConvertInfo,
    val result: Double
): Response

data class LiveResponse(
    override val success: Boolean,
    override val terms: String,
    override val privacy: String,
    val timestamp: Long,
    val source: String?,
    val quotes: Map<String, Double>
): Response


data class ConvertQuery(
    val from: String,
    val to: String,
    val amount: Double
)

data class LiveQuery(
    val source: String?,
    val currencies: String?
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

    fun convert(from: String, to: String, amount: Double): String {
        val (request, response, result) = Endpoints.CONVERT.httpGet(
            parameters = listOf(
                ParamFields.ACCESS_KEY to accessKey,
                ParamFields.FROM to from,
                ParamFields.TO to to,
                ParamFields.AMOUNT to amount
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

    fun convertAsync(from: String, to: String, amount: Double): CompletableFuture<ConvertResponse> {
        return calculateAsync(CompletableFuture<ConvertResponse>()) { future ->
            convertAsyncBlock(future, ConvertQuery(from, to, amount))
        }
    }

    fun live(currencies: String? = null, source: String? = null): LiveResponse {
        val (request, response, result) = Endpoints.LIVE.httpGet(
                parameters = createLiveParams(currencies, source))
            .responseString()

        when (result) {
            is Result.Success -> {
                return jsonMapper.readValue(result.value, jacksonTypeRef<LiveResponse>())
            }
            is Result.Failure -> {
                throw result.error
            }
        }
    }

    fun liveAsync(currencies: String? = null, source: String? = null): CompletableFuture<LiveResponse> {
        return calculateAsync(CompletableFuture<LiveResponse>()) { future ->
            liveAsyncBlock(future, currencies, source)
        }
    }

    private fun createLiveParams(currencies: String? = null, source: String? = null): List<Pair<String, String?>> {
        val params = mutableListOf(
                ParamFields.ACCESS_KEY to accessKey,
                ParamFields.SOURCE to source
        )
        if (currencies != null) params.add(ParamFields.CURRENCIES to currencies)
        if (source != null) params.add(ParamFields.SOURCE to source)
        return params
    }

    private fun <T>calculateAsync(future: CompletableFuture<T>, block: (future: CompletableFuture<T>) -> CompletableFuture<T>): CompletableFuture<T> {
        cachedThreadPool.submit { block(future) }
        return future
    }

    private fun liveAsyncBlock(future: CompletableFuture<LiveResponse>, currencies: String? = null, source: String? = null): CompletableFuture<LiveResponse> {
        Endpoints.LIVE.httpGet(
                parameters = createLiveParams(currencies, source)
            )
            .responseString { request, response, result ->
                when (result) {
                    is Result.Success -> {
                        val resultObject = jsonMapper.readValue<LiveResponse>(result.get(), jacksonTypeRef<LiveResponse>())
                        future.complete(resultObject)
                    }
                    is Result.Failure -> {
                        future.completeExceptionally(result.error)
                    }
                }
            }
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
                            val resultObject = jsonMapper.readValue<ListResponse>(result.get(), jacksonTypeRef<ListResponse>())
                            future.complete(resultObject)
                        }
                        is Result.Failure -> {
                            future.completeExceptionally(result.error)
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
                        val resultObject = jsonMapper.readValue<ConvertResponse>(result.get(), jacksonTypeRef<ConvertResponse>())
                        future.complete(resultObject)
                    }
                    is Result.Failure -> {
                        future.completeExceptionally(result.error)
                    }
                }
            }
        return future
    }
}
