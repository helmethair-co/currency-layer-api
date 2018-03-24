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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

class HistoricalTest {
    private CurrencyLayerApi currencyLayerApi = new CurrencyLayerApi("", true, Executors.newCachedThreadPool());
    private String SUCCESSFUL_RESPONSE_MESSAGE = "{\"success\":true,\"terms\":\"https://currencylayer.com/terms\",\"privacy\":\"https://currencylayer.com/privacy\",\"historical\":true,\"date\":\"2008-03-25\",\"timestamp\":1206489599,\"source\":\"HUF\",\"quotes\":{\"HUFUSD\":0.006052,\"HUFEUR\":0.003882,\"HUFGBP\":0.00303}}";

    @Test
    @DisplayName("Historical Success Test")
    void historicalSuccessTest() throws IOException {
        Client mockClient = mock(Client.class);
        FuelManager.Companion.getInstance().setClient(mockClient);
        com.github.kittinunf.fuel.core.Response response = TestHelper.createFuelResponse(SUCCESSFUL_RESPONSE_MESSAGE);
        when(mockClient.executeRequest(any())).thenReturn(response);

        HistoricalResponse historicalResponse = currencyLayerApi.historical(new Date(1206403201000L), "USD,EUR,GBP", "HUF");

        JsonNode responseObject = TestHelper.objectMapper.readTree(SUCCESSFUL_RESPONSE_MESSAGE);
        assertEquals(responseObject.findValue("quotes").toString(), TestHelper.objectMapper.writeValueAsString(historicalResponse.getQuotes()));
        assertEquals(responseObject.findValue("source").toString(), TestHelper.objectMapper.writeValueAsString(historicalResponse.getSource()));
    }

    @Test
    @DisplayName("Historical Async Success Test")
    void historicalAsyncSuccess() throws IOException, ExecutionException, InterruptedException, ParseException {
        Client mockClient = mock(Client.class);
        FuelManager.Companion.getInstance().setClient(mockClient);
        Response response = TestHelper.createFuelResponse(SUCCESSFUL_RESPONSE_MESSAGE);
        when(mockClient.executeRequest(any())).thenReturn(response);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        CompletableFuture<HistoricalResponse> future = currencyLayerApi.historicalAsync(dateFormat.parse("2008-03-25"), "USD,EUR,GBP", "HUF");
        HistoricalResponse historicalResponse = future.get();

        JsonNode responseObject = TestHelper.objectMapper.readTree(SUCCESSFUL_RESPONSE_MESSAGE);
        assertEquals(responseObject.findValue("quotes").toString(), TestHelper.objectMapper.writeValueAsString(historicalResponse.getQuotes()));
        assertEquals(responseObject.findValue("source").toString(), TestHelper.objectMapper.writeValueAsString(historicalResponse.getSource()));
    }
}
