package co.helmethair.currencylayer

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.Fuel
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


private object ParamFields {
    const val ACCESS_KEY = "access_key"
    const val FROM = "from"
    const val TO = "to"
    const val AMOUNT = "amount"
    const val CURRENCIES = "currencies"
    const val SOURCE = "source"
    const val DATE = "date"
}

private object Endpoints {
    const val LIST = "list"
    const val LIVE = "live"
    const val CONVERT = "convert"
    const val HISTORICAL = "historical"
}

private interface Response {
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

/**
 * Client API for wrapping the JSON API of currencylayer.com. [useSecureConnection] is available for paying users.
 */
class CurrencyLayerApi(
    private val accessKey: String,
    private val useSecureConnection: Boolean,
    private val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool()
) {
    private val jsonMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    private val protocol =  if (useSecureConnection) "https" else "http"
    private val basePath = "$protocol://apilayer.net/api/"

    /**
     * Returns a [ListResponse] object with all the supported currencies.
     */
    fun list(): ListResponse {
        val (request, response, result) = listRequest().responseString()
        return handleResponse(result, jacksonTypeRef<ListResponse>())
    }

    /**
     * The async version of the [list] function, returning a [CompletableFuture], call the .get()
     * method when you need the result.
     */
    fun listAsync(): CompletableFuture<ListResponse> {
        return calculateAsync(CompletableFuture<ListResponse>()) { future ->
            listAsyncBlock(future)
        }
    }

    /**
     * Returns a [ConvertResponse] object with result for a [from] and a [to] currency in the given [amount].
     */
    fun convert(from: String, to: String, amount: Double): ConvertResponse {
        val (request, response, result) = convertRequest(from, to, amount).responseString()
        return handleResponse(result, jacksonTypeRef<ConvertResponse>())

    }

    /**
     * The async version of the [convert] function, returning a [CompletableFuture], call the .get()
     * method when you need the result.
     */
    fun convertAsync(from: String, to: String, amount: Double): CompletableFuture<ConvertResponse> {
        return calculateAsync(CompletableFuture<ConvertResponse>()) { future ->
            convertAsyncBlock(future, from, to, amount)
        }
    }

    /**
     * Returns a [LiveResponse] object containing the live exchange rates for a given [source] currency.
     * The default value is "USD". The number of [currencies] can be limited by providing a string
     * containing the currencies separated by a comma: "EUR,GBP,HUF".
     */
    fun live(currencies: String? = null, source: String? = null): LiveResponse {
        val (request, response, result) = liveRequest(currencies, source).responseString()
        return handleResponse(result, jacksonTypeRef<LiveResponse>())
    }

    /**
     * The async version of the [live] function, returning a [CompletableFuture], call the .get()
     * method when you need the result.
     */
    fun liveAsync(currencies: String? = null, source: String? = null): CompletableFuture<LiveResponse> {
        return calculateAsync(CompletableFuture<LiveResponse>()) { future ->
            liveAsyncBlock(future, currencies, source)
        }
    }

    /**
     * Returns a [HistoricalResponse] object containing the historical exchange rates for a given [source] currency,
     * in a specific [date]. The default value is "USD". The number of [currencies] can be limited by providing a
     * string containing the currencies separated by a comma: "EUR,GBP,HUF".
     */
    fun historical(date: Date, currencies: String? = null, source: String? = null): HistoricalResponse {
        val (request, response, result) = historicalRequest(date, currencies, source).responseString()
        return handleResponse(result, jacksonTypeRef<HistoricalResponse>())

    }

    /**
     * The async version of the [historical] function, returning a [CompletableFuture], call the .get()
     * method when you need the result.
     */
    fun historicalAsync(date: Date, currencies: String? = null, source: String? = null): CompletableFuture<HistoricalResponse> {
        return calculateAsync(CompletableFuture<HistoricalResponse>()) { future ->
            historicalAsyncBlock(future, date, currencies, source)
        }
    }

    private fun listRequest(): Request {
        return Fuel.get("$basePath${Endpoints.LIST}",
                parameters = listOf(
                    ParamFields.ACCESS_KEY to accessKey
                )
            )
    }

    private fun convertRequest(from: String, to: String, amount: Double): Request {
        return Fuel.get("$basePath${Endpoints.CONVERT}",
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
            ParamFields.ACCESS_KEY to accessKey
        )
        if (currencies != null) params.add(ParamFields.CURRENCIES to currencies)
        if (source != null) params.add(ParamFields.SOURCE to source)
        return Fuel.get("$basePath${Endpoints.LIVE}", parameters = params)
    }

    private fun historicalRequest(date: Date, currencies: String? = null, source: String? = null): Request {
        val params = mutableListOf(
            ParamFields.ACCESS_KEY to accessKey,
            ParamFields.DATE to dateFormat.format(date)
        )
        if (currencies != null) params.add(ParamFields.CURRENCIES to currencies)
        if (source != null) params.add(ParamFields.SOURCE to source)
        return Fuel.get("$basePath${Endpoints.HISTORICAL}", parameters = params)
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
