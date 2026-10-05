package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final String CONTENT_TYPE =
            "application/json; charset=UTF-8";

    protected final Gson gson = new Gson();

    protected void sendJson(
            HttpExchange exchange,
            int status,
            Object body
    ) throws IOException {

        String json = gson.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set("Content-Type", CONTENT_TYPE);

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    protected void sendNoContent(
            HttpExchange exchange
    ) throws IOException {

        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    protected void sendError(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {

        ErrorResponse response =
                new ErrorResponse(message, List.of());

        sendJson(exchange, status, response);
    }
}