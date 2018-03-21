package co.helmethair.currencylayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kittinunf.fuel.core.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TestHelper {
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static Response createFuelResponse(String responseMessage) throws IOException {
        URL url = new URL("http://example.com/");
        InputStream stream = new ByteArrayInputStream(responseMessage.getBytes(StandardCharsets.UTF_8));
        return new Response(url, 200, responseMessage, new HashMap<>(), 0L, stream);
    }
}
