package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ExecutorService threadPool;
    private final List<String> validPaths;
    private final int port;

    public Server(int port, int pool_size) {
        this.port = port;
        // Инициализация пула потоков с размером, переданным в конструктор
        this.threadPool = Executors.newFixedThreadPool(pool_size);
        // Список допустимых путей
        this.validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    }

    // Метод для запуска сервера
    public void start() {
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while (true) {
                // Блокирующий вызов, ожидающий подключения клиента
                final var socket = serverSocket.accept();
                System.out.println("Принято новое соединение от: " + socket.getInetAddress());
                // Каждое новое соединение передается в пул потоков для обработки
                threadPool.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            System.err.println("Ошибка при запуске или работе ServerSocket: " + e.getMessage());
            threadPool.shutdown(); // Остановка пула потоков при ошибке
        }
    }

    // Метод для обработки конкретного подключения в отдельном потоке
    private void handleConnection(Socket socket) {
        try (
                socket; // Автоматическое закрытие сокета при выходе из try
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            // 1. Чтение строки запроса
            final var requestLine = in.readLine();
            if (requestLine == null) {
                // Пустой запрос, просто закрываем соединение
                return;
            }

            final var parts = requestLine.split(" ");

            // Проверка формата запроса (минимум 3 части: метод, путь, версия HTTP)
            if (parts.length != 3) {
                return;
            }

            final var path = parts[1];

            // 2. Проверка пути
            if (!validPaths.contains(path)) {
                sendNotFoundResponse(out);
                return;
            }

            // 3. Обработка файла
            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // 4. Специальный случай: /classic.html с динамическим контентом
            if (path.equals("/classic.html")) {
                handleClassicHtml(filePath, mimeType, out);
                return;
            }

            // 5. Стандартная обработка (отправка статического файла)
            sendFileResponse(filePath, mimeType, out);

        } catch (IOException e) {
            // Логирование ошибки обработки соединения
            System.err.println("Ошибка при обработке соединения: " + e.getMessage());
            // StackTrace лучше оставить для отладки, но для продакшена можно заменить на более простое логирование
        }
    }

    // Приватный метод для отправки 404 Not Found
    private void sendNotFoundResponse(BufferedOutputStream out) throws IOException {
        final String response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes());
        out.flush();
    }

    // Приватный метод для обработки /classic.html (динамический контент)
    private void handleClassicHtml(Path filePath, String mimeType, BufferedOutputStream out) throws IOException {
        final var template = Files.readString(filePath);
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();

        final String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        out.write(responseHeaders.getBytes());
        out.write(content);
        out.flush();
    }

    // Приватный метод для отправки статических файлов
    private void sendFileResponse(Path filePath, String mimeType, BufferedOutputStream out) throws IOException {
        final var length = Files.size(filePath);

        final String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        out.write(responseHeaders.getBytes());
        // Копирование содержимого файла напрямую в выходной поток
        Files.copy(filePath, out);
        out.flush();
    }
}