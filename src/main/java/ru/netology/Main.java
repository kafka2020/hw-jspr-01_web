package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    private static final int PORT = 9999;
    private static final int POOL_SIZE = 64;

    public static void main(String[] args) {

        final var server = new Server(PORT, POOL_SIZE);

        // Добавление хендлера для GET /messages
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                try {
                    final var response = "Hello from GET /messages";
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Length: " + response.length() + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    responseStream.write(response.getBytes());
                    responseStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Добавление хендлера для POST /messages
        // Пример с Лямбдой
        server.addHandler("POST", "/messages", (request, responseStream) -> {
            try {
                final var response = "Hello from POST /messages";
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Length: " + response.length() + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(response.getBytes());
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Инициализация обработки статических файлов (восстанавливаем логику из первой версии)
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

        for (String path : validPaths) {
            server.addHandler("GET", path, (request, responseStream) -> {
                try {
                    final var filePath = Path.of(".", "public", request.getPath());
                    final var mimeType = Files.probeContentType(filePath);

                    // Особый случай для classic.html
                    if (request.getPath().equals("/classic.html")) {
                        final var template = Files.readString(filePath);
                        final var content = template.replace(
                                "{time}",
                                LocalDateTime.now().toString()
                        ).getBytes();
                        responseStream.write((
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: " + mimeType + "\r\n" +
                                        "Content-Length: " + content.length + "\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        responseStream.write(content);
                        responseStream.flush();
                        return;
                    }

                    // Стандартная обработка статики
                    final var length = Files.size(filePath);
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(filePath, responseStream);
                    responseStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        // Запуск сервера
        server.start();
    }
}