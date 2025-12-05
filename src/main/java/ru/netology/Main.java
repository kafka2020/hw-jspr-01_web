package ru.netology;

public class Main {
    private static final int PORT = 9999;
    private static final int POOL_SIZE = 64;

    public static void main(String[] args) {
        final var server = new Server(PORT, POOL_SIZE);
        server.start();
    }
}