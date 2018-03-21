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

class ListTest {
    private CurrencyLayerApi currencyLayerApi = new CurrencyLayerApi("", true, Executors.newCachedThreadPool());
    private String SUCCESSFUL_RESPONSE_MESSAGE = "{\"success\":true,\"terms\":\"https://currencylayer.com/terms\",\"privacy\":\"https://currencylayer.com/privacy\",\"currencies\":{\"AED\":\"United Arab Emirates Dirham\",\"AFN\":\"Afghan Afghani\",\"ALL\":\"Albanian Lek\"}}";

    @Test
    @DisplayName("List Success Test")
    void listSuccessTest() throws IOException {
        Client mockClient = mock(Client.class);
        FuelManager.Companion.getInstance().setClient(mockClient);
        Response response = TestHelper.createFuelResponse(SUCCESSFUL_RESPONSE_MESSAGE);
        when(mockClient.executeRequest(any())).thenReturn(response);

        ListResponse listResponse = currencyLayerApi.list();

        JsonNode responseObject = TestHelper.objectMapper.readTree(SUCCESSFUL_RESPONSE_MESSAGE);
        assertEquals(responseObject.findValue("currencies").toString(), TestHelper.objectMapper.writeValueAsString(listResponse.getCurrencies()));
    }

    @Test
    @DisplayName("List Async Success Test")
    void listAsyncSuccess() throws IOException, ExecutionException, InterruptedException {
        Client mockClient = mock(Client.class);
        FuelManager.Companion.getInstance().setClient(mockClient);
        Response response = TestHelper.createFuelResponse(SUCCESSFUL_RESPONSE_MESSAGE);
        when(mockClient.executeRequest(any())).thenReturn(response);

        CompletableFuture<ListResponse> future = currencyLayerApi.listAsync();
        ListResponse listResponse = future.get();

        JsonNode responseObject = TestHelper.objectMapper.readTree(SUCCESSFUL_RESPONSE_MESSAGE);
        assertEquals(responseObject.findValue("currencies").toString(), TestHelper.objectMapper.writeValueAsString(listResponse.getCurrencies()));
    }
}
