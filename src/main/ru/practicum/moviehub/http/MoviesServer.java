package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {

    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;

        try {
            this.server = HttpServer.create(
                    new InetSocketAddress(port),
                    0
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Не удалось создать HTTP-сервер",
                    e
            );
        }

        server.createContext(
                "/movies",
                new MoviesHandler(store)
        );
    }

    public void start() {
        server.start();
        System.out.println(
                "MovieHub запущен на http://localhost:8080"
        );
    }

    public void stop() {
        server.stop(0);
        System.out.println("MovieHub остановлен");
    }

    public MoviesStore getStore() {
        return store;
    }
}