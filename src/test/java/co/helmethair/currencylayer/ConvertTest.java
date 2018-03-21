package co.helmethair.currencylayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.kittinunf.fuel.core.Client;
import com.github.kittinunf.fuel.core.FuelManager;
import com.github.kittinunf.fuel.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

class ConvertTest {
    private CurrencyLayerApi currencyLayerApi = new CurrencyLayerApi("", true, Executors.newCachedThreadPool());
    private String SUCCESSFUL_RESPONSE_MESSAGE = "{\"success\":true,\"terms\":\"https://currencylayer.com/terms\",\"privacy\":\"https://currencylayer.com/privacy\",\"currencies\":{\"AED\":\"United Arab Emirates Dirham\",\"AFN\":\"Afghan Afghani\",\"ALL\":\"Albanian Lek\"}}";

    @Test
    @DisplayName("Convert Success Test")
    void convertSuccessTest() throws IOException {
        Client mockClient = mock(Client.class);
        FuelManager.Companion.getInstance().setClient(mockClient);
        Response response = TestHelper.createFuelResponse(SUCCESSFUL_RESPONSE_MESSAGE);
        when(mockClient.executeRequest(any())).thenReturn(response);

        ConvertResponse convertResponse = currencyLayerApi.convert("USD", "HUF", 10.0);

        JsonNode responseObject = TestHelper.objectMapper.readTree(SUCCESSFUL_RESPONSE_MESSAGE);
        assertEquals(responseObject.findValue("result").asDouble(), convertResponse.getResult());
    }

    @Test
    @DisplayName("Convert Async Success Test")
    void convertAsyncSuccess() throws IOException, ExecutionException, InterruptedException {
        Client mockClient = mock(Client.class);
        FuelManager.Companion.getInstance().setClient(mockClient);
        Response response = TestHelper.createFuelResponse(SUCCESSFUL_RESPONSE_MESSAGE);
        when(mockClient.executeRequest(any())).thenReturn(response);

        CompletableFuture<ConvertResponse> future = currencyLayerApi.convertAsync("USD", "HUF", 10.0);
        ConvertResponse convertResponse = future.get();

        JsonNode responseObject = TestHelper.objectMapper.readTree(SUCCESSFUL_RESPONSE_MESSAGE);
        assertEquals(responseObject.findValue("result").asDouble(), convertResponse.getResult());
    }
}
