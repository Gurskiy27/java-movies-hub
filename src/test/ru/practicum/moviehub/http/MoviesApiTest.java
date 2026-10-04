package ru.practicum.moviehub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.http.MoviesServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(
                new ru.practicum.moviehub.store.MoviesStore(),
                8080
        );

        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @Test
    void shouldReturnEmptyMoviesList() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(200, response.statusCode());
        assertEquals(
                "application/json; charset=UTF-8",
                response.headers()
                        .firstValue("Content-Type")
                        .orElse("")
        );
        assertEquals("[]", response.body());
    }

    @Test
    void shouldCreateMovie() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "title": "Интерстеллар",
                                  "year": 2014
                                }
                                """,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"id\""));
        assertTrue(response.body().contains("Интерстеллар"));
        assertTrue(response.body().contains("2014"));
    }

    @Test
    void shouldReturnMoviesAfterCreation() throws Exception {

        createMovie("Интерстеллар", 2014);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Интерстеллар"));
    }

    @Test
    void shouldReturnMovieById() throws Exception {

        createMovie("Интерстеллар", 2014);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/1"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Интерстеллар"));
        assertTrue(response.body().contains("\"id\":1"));
    }

    @Test
    void shouldReturn404ForUnknownMovie() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/999"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(404, response.statusCode());
    }

    @Test
    void shouldReturn400ForInvalidMovieId() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(400, response.statusCode());
    }

    @Test
    void shouldDeleteMovie() throws Exception {

        createMovie("Интерстеллар", 2014);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(204, response.statusCode());
    }

    @Test
    void shouldReturn404WhenDeletingUnknownMovie() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(404, response.statusCode());
    }

    @Test
    void shouldFilterMoviesByYear() throws Exception {

        createMovie("Интерстеллар", 2014);
        createMovie("Матрица", 1999);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies?year=2014"
                ))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Интерстеллар"));
        assertTrue(!response.body().contains("Матрица"));
    }

    @Test
    void shouldReturnEmptyListForUnknownYear() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies?year=2000"
                ))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void shouldReturn400ForInvalidYear() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/movies?year=abc"
                ))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(400, response.statusCode());
    }

    @Test
    void shouldReturn422ForEmptyTitle() throws Exception {

        HttpResponse<String> response =
                createMovieRequest(
                        """
                        {
                          "title": "",
                          "year": 2014
                        }
                        """
                );

        assertEquals(422, response.statusCode());
    }

    @Test
    void shouldReturn422ForTooLongTitle() throws Exception {

        String title = "А".repeat(101);

        HttpResponse<String> response =
                createMovieRequest(
                        """
                        {
                          "title": "%s",
                          "year": 2014
                        }
                        """.formatted(title)
                );

        assertEquals(422, response.statusCode());
    }

    @Test
    void shouldReturn422ForInvalidYear() throws Exception {

        HttpResponse<String> response =
                createMovieRequest(
                        """
                        {
                          "title": "Фильм",
                          "year": 1800
                        }
                        """
                );

        assertEquals(422, response.statusCode());
    }

    @Test
    void shouldReturn415ForWrongContentType() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "text/plain")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                "test",
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(415, response.statusCode());
    }

    @Test
    void shouldReturn422ForInvalidJson() throws Exception {

        HttpResponse<String> response =
                createMovieRequest(
                        """
                        {
                          "title": "Фильм",
                          "year":
                        """
                );

        assertEquals(422, response.statusCode());
    }

    @Test
    void shouldReturn405ForUnsupportedMethod() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .method("PUT", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

        assertEquals(405, response.statusCode());
    }

    private void createMovie(String title, int year)
            throws Exception {

        createMovieRequest(
                """
                {
                  "title": "%s",
                  "year": %d
                }
                """.formatted(title, year)
        );
    }

    private HttpResponse<String> createMovieRequest(
            String body
    ) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                body,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );
    }
}