package co.helmethair

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.math.BigDecimal

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

class CurrencyLayerApi(private val accessKey: String) {
    companion object {
        private const val BASE_URL = "http://apilayer.net/api/"
    }

    init {
        FuelManager.instance.basePath = BASE_URL
    }

    fun list(): String {
        val (request, response, result) = Endpoints.LIST.httpGet(
            parameters = listOf(
                ParamFields.ACCESS_KEY to accessKey
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

    fun listAsync(onSuccess: (result: String) -> Unit, onError: (e: Exception) -> Unit) {
        Endpoints.LIST.httpGet(
            parameters = listOf(
                    ParamFields.ACCESS_KEY to accessKey
            ))
            .responseString { request, response, result ->
                when (result) {
                    is Result.Success -> {
                        onSuccess(result.get())
                    }
                    is Result.Failure -> {
                        onError(result.getException())
                    }
                }
            }
    }

    fun convert(amount: BigDecimal, fromCurrency: String, toCurrency: String): String {
        val (request, response, result) = Endpoints.CONVERT.httpGet(
            parameters = listOf(
                ParamFields.ACCESS_KEY to accessKey,
                ParamFields.FROM to fromCurrency,
                ParamFields.TO to toCurrency,
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

    fun convertAsync(amount: BigDecimal, fromCurrency: String, toCurrency: String, onSuccess: (result: String) -> Unit, onError: (e: Exception) -> Unit) {
        Endpoints.CONVERT.httpGet(
            parameters = listOf(
                ParamFields.ACCESS_KEY to accessKey,
                ParamFields.FROM to fromCurrency,
                ParamFields.TO to toCurrency,
                ParamFields.AMOUNT to amount
            ))
            .responseString { request, response, result ->
                when (result) {
                    is Result.Success -> {
                        onSuccess(result.get())
                    }
                    is Result.Failure -> {
                        onError(result.getException())
                    }
                }
            }
    }
}
