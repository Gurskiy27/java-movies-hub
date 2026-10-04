package ru.practicum.moviehub.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (!path.equals("/movies")
                && !path.startsWith("/movies/")) {

            sendError(exchange, 404, "Ресурс не найден");
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange, path);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange, path);
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            handleDelete(exchange, path);
            return;
        }

        sendError(exchange, 405, "Метод не поддерживается");
    }

    private void handleGet(
            HttpExchange exchange,
            String path
    ) throws IOException {

        if (path.equals("/movies")) {

            String query =
                    exchange.getRequestURI().getRawQuery();

            if (query == null || query.isEmpty()) {
                sendJson(exchange, 200, store.findAll());
                return;
            }

            handleYearFilter(exchange, query);
            return;
        }

        String idPart =
                path.substring("/movies/".length());

        if (idPart.isEmpty()) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        Movie movie = store.findById(id);

        if (movie == null) {
            sendError(exchange, 404, "Фильм не найден");
            return;
        }

        sendJson(exchange, 200, movie);
    }

    private void handleYearFilter(
            HttpExchange exchange,
            String query
    ) throws IOException {

        String yearValue = null;

        for (String parameter : query.split("&")) {

            String[] parts = parameter.split("=", 2);

            if (parts.length == 2
                    && parts[0].equals("year")) {

                yearValue = parts[1];
            }
        }

        if (yearValue == null || yearValue.isEmpty()) {
            sendError(
                    exchange,
                    400,
                    "Некорректный параметр запроса — 'year'"
            );
            return;
        }

        int year;

        try {
            year = Integer.parseInt(yearValue);
        } catch (NumberFormatException e) {
            sendError(
                    exchange,
                    400,
                    "Некорректный параметр запроса — 'year'"
            );
            return;
        }

        sendJson(
                exchange,
                200,
                store.findByYear(year)
        );
    }

    private void handlePost(
            HttpExchange exchange,
            String path
    ) throws IOException {

        if (!path.equals("/movies")) {
            sendError(exchange, 404, "Ресурс не найден");
            return;
        }

        String contentType =
                exchange.getRequestHeaders()
                        .getFirst("Content-Type");

        if (contentType == null
                || !contentType
                .toLowerCase()
                .startsWith("application/json")) {

            sendError(
                    exchange,
                    415,
                    "Неподдерживаемый Content-Type"
            );
            return;
        }

        String body;

        try (InputStream input =
                     exchange.getRequestBody()) {

            body = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        JsonObject json;

        try {
            json = JsonParser.parseString(body)
                    .getAsJsonObject();
        } catch (JsonParseException
                 | IllegalStateException e) {

            sendValidationError(
                    exchange,
                    List.of("Некорректный JSON")
            );
            return;
        }

        String title = null;
        Integer year = null;

        if (json.has("title")
                && !json.get("title").isJsonNull()) {

            try {
                title = json.get("title").getAsString();
            } catch (Exception e) {
                sendValidationError(
                        exchange,
                        List.of(
                                "Некорректное значение поля 'title'"
                        )
                );
                return;
            }
        }

        if (json.has("year")
                && !json.get("year").isJsonNull()) {

            try {
                year = json.get("year").getAsInt();
            } catch (Exception e) {
                sendValidationError(
                        exchange,
                        List.of(
                                "Некорректное значение поля 'year'"
                        )
                );
                return;
            }
        }

        List<String> errors = new ArrayList<>();

        if (title == null
                || title.trim().isEmpty()) {

            errors.add(
                    "название не должно быть пустым"
            );

        } else if (title.length() > 100) {

            errors.add(
                    "название не должно быть длиннее 100 символов"
            );
        }

        int maxYear = Year.now().getValue() + 1;

        if (year == null) {

            errors.add("год должен быть указан");

        } else if (year < 1888 || year > maxYear) {

            errors.add(
                    "год должен быть между 1888 и "
                            + maxYear
            );
        }

        if (!errors.isEmpty()) {
            sendValidationError(exchange, errors);
            return;
        }

        Movie movie = store.add(title, year);

        sendJson(exchange, 201, movie);
    }

    private void handleDelete(
            HttpExchange exchange,
            String path
    ) throws IOException {

        if (!path.startsWith("/movies/")) {
            sendError(exchange, 404, "Фильм не найден");
            return;
        }

        String idPart =
                path.substring("/movies/".length());

        int id;

        try {
            id = Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        Movie removed = store.delete(id);

        if (removed == null) {
            sendError(exchange, 404, "Фильм не найден");
            return;
        }

        sendNoContent(exchange);
    }

    private void sendValidationError(
            HttpExchange exchange,
            List<String> errors
    ) throws IOException {

        ErrorResponse response =
                new ErrorResponse(
                        "Ошибка валидации",
                        errors
                );

        sendJson(exchange, 422, response);
    }
}