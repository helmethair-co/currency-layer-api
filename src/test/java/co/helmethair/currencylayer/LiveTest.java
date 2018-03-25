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

class LiveTest {
    private CurrencyLayerApi currencyLayerApi = new CurrencyLayerApi("", true, Executors.newCachedThreadPool());
    private String SUCCESSFUL_RESPONSE_MESSAGE = "{\"success\":true,\"terms\":\"https://currencylayer.com/terms\",\"privacy\":\"https://currencylayer.com/privacy\",\"timestamp\":1521924846,\"source\":\"HUF\",\"quotes\":{\"HUFUSD\":0.003951,\"HUFEUR\":0.003196,\"HUFGBP\":0.002796}}";

    @Test
    @DisplayName("Live Success Test")
    void listSuccessTest() throws IOException {
        Client mockClient = mock(Client.class);
        FuelManager.Companion.getInstance().setClient(mockClient);
        com.github.kittinunf.fuel.core.Response response = TestHelper.createFuelResponse(SUCCESSFUL_RESPONSE_MESSAGE);
        when(mockClient.executeRequest(any())).thenReturn(response);

        LiveResponse liveResponse = currencyLayerApi.live("USD,EUR,GBP", "HUF");

        JsonNode responseObject = TestHelper.objectMapper.readTree(SUCCESSFUL_RESPONSE_MESSAGE);
        assertEquals(responseObject.findValue("quotes").toString(), TestHelper.objectMapper.writeValueAsString(liveResponse.getQuotes()));
        assertEquals(responseObject.findValue("source").toString(), TestHelper.objectMapper.writeValueAsString(liveResponse.getSource()));
    }

    @Test
    @DisplayName("Live Async Success Test")
    void liveAsyncSuccess() throws IOException, ExecutionException, InterruptedException {
        Client mockClient = mock(Client.class);
        FuelManager.Companion.getInstance().setClient(mockClient);
        Response response = TestHelper.createFuelResponse(SUCCESSFUL_RESPONSE_MESSAGE);
        when(mockClient.executeRequest(any())).thenReturn(response);

        CompletableFuture<LiveResponse> future = currencyLayerApi.liveAsync("USD,EUR,GBP", "HUF");
        LiveResponse liveResponse = future.get();

        JsonNode responseObject = TestHelper.objectMapper.readTree(SUCCESSFUL_RESPONSE_MESSAGE);
        assertEquals(responseObject.findValue("quotes").toString(), TestHelper.objectMapper.writeValueAsString(liveResponse.getQuotes()));
        assertEquals(responseObject.findValue("source").toString(), TestHelper.objectMapper.writeValueAsString(liveResponse.getSource()));
    }
}
