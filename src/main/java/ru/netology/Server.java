package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final ExecutorService threadPool;
    // Ключ - метод (GET, POST), Значение - Map (Ключ - путь, Значение - Handler)
    private final Map<String, Map<String, Handler>> handlers;

    public Server(int port, int poolSize) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
        this.handlers = new ConcurrentHashMap<>();
    }

    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while (true) {
                final var socket = serverSocket.accept();
                System.out.println("Принято новое соединение от: " + socket.getInetAddress());
                threadPool.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket socket) {
        try (
                socket;
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
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

            final var method = parts[0];
            final var path = parts[1];

            // Читаем заголовки (просто вычитываем до пустой строки)
            final var headers = new ArrayList<String>();
            String line;
            while (!(line = in.readLine()).equals("")) {
                headers.add(line);
            }

            // Формируем объект Request
            // Примечание: для полноценной работы с body при использовании BufferedReader
            // требуется более сложная логика, но в рамках упрощения передаем raw stream.
            final var request = new Request(method, path, headers, socket.getInputStream());

            // Поиск хендлера
            var handlerMap = handlers.get(request.getMethod());
            if (handlerMap == null) {
                sendNotFound(out);
                return;
            }

            var handler = handlerMap.get(request.getPath());
            if (handler == null) {
                sendNotFound(out);
                return;
            }

            // Запуск обработчика
            handler.handle(request, out);

        } catch (IOException e) {
            System.err.println("Ошибка при обработке соединения: " + e.getMessage());
        }
    }

    private void sendNotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}