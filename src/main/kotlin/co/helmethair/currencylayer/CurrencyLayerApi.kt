package co.helmethair.currencylayer

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


object ParamFields {
    const val ACCESS_KEY = "access_key"
    const val FROM = "from"
    const val TO = "to"
    const val AMOUNT = "amount"
    const val CURRENCIES = "currencies"
    const val SOURCE = "source"
    const val DATE = "date"
}

object Endpoints {
    const val LIST = "list"
    const val LIVE = "live"
    const val CONVERT = "convert"
    const val HISTORICAL = "historical"
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

data class HistoricalResponse(
    override val success: Boolean,
    override val terms: String,
    override val privacy: String,
    val historical: Boolean,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val date: Date,
    val timestamp: Long,
    val source: String?,
    val quotes: Map<String, Double>
): Response

data class ConvertQuery(
    val from: String,
    val to: String,
    val amount: Double
)

data class ConvertInfo(
    val timestamp: Long,
    val quote: Double
)

class CurrencyLayerApi(
    private val accessKey: String,
    private val useSecureConnection: Boolean,
    private val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool()
) {
    private val jsonMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    init {
        val protocol = if (useSecureConnection) "https" else "http"
        FuelManager.instance.basePath = "$protocol://apilayer.net/api/"
    }

    fun list(): ListResponse {
        val (request, response, result) = listRequest().responseString()
        return handleResponse(result, jacksonTypeRef<ListResponse>())
    }

    fun listAsync(): CompletableFuture<ListResponse> {
        return calculateAsync(CompletableFuture<ListResponse>()) { future ->
            listAsyncBlock(future)
        }
    }

    fun convert(from: String, to: String, amount: Double): ConvertResponse {
        val (request, response, result) = convertRequest(from, to, amount).responseString()
        return handleResponse(result, jacksonTypeRef<ConvertResponse>())

    }

    fun convertAsync(from: String, to: String, amount: Double): CompletableFuture<ConvertResponse> {
        return calculateAsync(CompletableFuture<ConvertResponse>()) { future ->
            convertAsyncBlock(future, from, to, amount)
        }
    }

    fun live(currencies: String? = null, source: String? = null): LiveResponse {
        val (request, response, result) = liveRequest(currencies, source).responseString()
        return handleResponse(result, jacksonTypeRef<LiveResponse>())
    }

    fun liveAsync(currencies: String? = null, source: String? = null): CompletableFuture<LiveResponse> {
        return calculateAsync(CompletableFuture<LiveResponse>()) { future ->
            liveAsyncBlock(future, currencies, source)
        }
    }

    fun historical(date: Date, currencies: String? = null, source: String? = null): HistoricalResponse {
        val (request, response, result) = historicalRequest(date, currencies, source).responseString()
        return handleResponse(result, jacksonTypeRef<HistoricalResponse>())

    }

    fun historicalAsync(date: Date, currencies: String? = null, source: String? = null): CompletableFuture<HistoricalResponse> {
        return calculateAsync(CompletableFuture<HistoricalResponse>()) { future ->
            historicalAsyncBlock(future, date, currencies, source)
        }
    }

    private fun listRequest(): Request {
        return Endpoints.LIST
            .httpGet(
                parameters = listOf(
                    ParamFields.ACCESS_KEY to accessKey
                )
            )
    }

    private fun convertRequest(from: String, to: String, amount: Double): Request {
        return Endpoints.CONVERT
            .httpGet(
                parameters = listOf(
                    ParamFields.ACCESS_KEY to accessKey,
                    ParamFields.FROM to from,
                    ParamFields.TO to to,
                    ParamFields.AMOUNT to amount
                )
            )
    }

    private fun liveRequest(currencies: String? = null, source: String? = null): Request {
        val params = mutableListOf(
            ParamFields.ACCESS_KEY to accessKey,
            ParamFields.SOURCE to source
        )
        if (currencies != null) params.add(ParamFields.CURRENCIES to currencies)
        if (source != null) params.add(ParamFields.SOURCE to source)
        return Endpoints.LIVE.httpGet(parameters = params)
    }

    private fun historicalRequest(date: Date, currencies: String? = null, source: String? = null): Request {
        val params = mutableListOf(
            ParamFields.ACCESS_KEY to accessKey,
            ParamFields.SOURCE to source,
            ParamFields.DATE to dateFormat.format(date)
        )
        if (currencies != null) params.add(ParamFields.CURRENCIES to currencies)
        if (source != null) params.add(ParamFields.SOURCE to source)
        return Endpoints.HISTORICAL.httpGet(parameters = params)
    }

    private fun <T>calculateAsync(future: CompletableFuture<T>, block: (future: CompletableFuture<T>) -> CompletableFuture<T>): CompletableFuture<T> {
        cachedThreadPool.submit { block(future) }
        return future
    }

    private fun <T>handleResponse(result: Result<String, FuelError>, typeReference: TypeReference<T>): T {
        when (result) {
            is Result.Success -> {
                return jsonMapper.readValue(result.value, typeReference)
            }
            is Result.Failure -> {
                throw result.error
            }
        }
    }

    private fun <T>createHandleResponse(future: CompletableFuture<T>, typeRef: TypeReference<T>): (Request, com.github.kittinunf.fuel.core.Response, Result<String, FuelError>) -> Unit {
        return { request, response, result ->
            when (result) {
                is Result.Success -> {
                    val resultObject = jsonMapper.readValue<T>(result.get(), typeRef)
                    future.complete(resultObject)
                }
                is Result.Failure -> {
                    future.completeExceptionally(result.error)
                }
            }
        }
    }

    private fun liveAsyncBlock(future: CompletableFuture<LiveResponse>, currencies: String? = null, source: String? = null): CompletableFuture<LiveResponse> {
        liveRequest(currencies, source).responseString(handler = createHandleResponse(future, jacksonTypeRef<LiveResponse>()))
        return future
    }

    private fun historicalAsyncBlock(future: CompletableFuture<HistoricalResponse>, date: Date, currencies: String? = null, source: String? = null): CompletableFuture<HistoricalResponse> {
        historicalRequest(date, currencies, source).responseString(handler = createHandleResponse(future, jacksonTypeRef<HistoricalResponse>()))
        return future
    }

    private fun listAsyncBlock(future: CompletableFuture<ListResponse>): CompletableFuture<ListResponse> {
        listRequest().responseString(handler = createHandleResponse(future, jacksonTypeRef<ListResponse>()))
        return future
    }

    private fun convertAsyncBlock(future: CompletableFuture<ConvertResponse>, from: String, to: String, amount: Double): CompletableFuture<ConvertResponse> {
        convertRequest(from, to, amount).responseString(handler = createHandleResponse(future, jacksonTypeRef<ConvertResponse>()))
        return future
    }
}
