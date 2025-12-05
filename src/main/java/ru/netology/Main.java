package ru.netology;

public class Main {
    // Порт, на котором будет запущен сервер
    private static final int PORT = 9999;

    public static void main(String[] args) {
        // Создаем экземпляр класса Server
        final var server = new Server(PORT);
        // Запускаем сервер
        server.start();
    }
}