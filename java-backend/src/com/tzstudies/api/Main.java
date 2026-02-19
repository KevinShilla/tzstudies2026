package com.tzstudies.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().getParent();
        if (projectRoot == null) projectRoot = Paths.get("..").toAbsolutePath().normalize();

        Path examsDir = projectRoot.resolve("exams");
        Path keysDir = projectRoot.resolve("answer_keys");

        // Health check
        server.createContext("/api/health", new JsonBytesHandler("{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8)));

        // List endpoints (return arrays of filenames)
        server.createContext("/api/exams", new ListFilesHandler(examsDir));
        server.createContext("/api/answer-keys", new ListFilesHandler(keysDir));

        // File download: /api/file/exams/<filename> or /api/file/answer-keys/<filename>
        server.createContext("/api/file", new FileDownloadHandler(examsDir, keysDir));

        // Simple landing
        server.createContext("/", new TextHandler(
                "TZ Studies Java API is running. Try /api/health, /api/exams, /api/answer-keys\n"
        ));

        server.setExecutor(null);
        server.start();

        System.out.println("TZ Studies Java API running on http://localhost:" + port);
        System.out.println("Project root: " + projectRoot);
        System.out.println("Exams dir:     " + examsDir);
        System.out.println("Answer keys:   " + keysDir);
    }

    // -------- Handlers --------

    private static class ListFilesHandler implements HttpHandler {
        private final Path dir;

        ListFilesHandler(Path dir) {
            this.dir = dir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            List<String> files = new ArrayList<String>();
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
                try {
                    for (Path p : stream) {
                        if (Files.isRegularFile(p)) {
                            files.add(p.getFileName().toString());
                        }
                    }
                } finally {
                    stream.close();
                }
            }

            Collections.sort(files);
            String json = toJsonArray(files);
            sendJson(exchange, 200, json);
        }
    }

    private static class FileDownloadHandler implements HttpHandler {
        private final Path examsDir;
        private final Path keysDir;

        FileDownloadHandler(Path examsDir, Path keysDir) {
            this.examsDir = examsDir;
            this.keysDir = keysDir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                sendNoContent(exchange);
                return;
            }

            if (!"GET".equalsIgnoreCase(method)) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            // Expected: /api/file/<type>/<filename>
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            // ["", "api", "file", "exams", "Some.pdf"]
            if (parts.length < 5) {
                sendText(exchange, 400, "Bad request. Use /api/file/exams/<filename> or /api/file/answer-keys/<filename>");
                return;
            }

            String type = parts[3];
            String filename = decodeUrl(joinFrom(parts, 4));

            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                sendText(exchange, 400, "Invalid filename");
                return;
            }

            Path base;
            if ("exams".equals(type)) {
                base = examsDir;
            } else if ("answer-keys".equals(type)) {
                base = keysDir;
            } else {
                sendText(exchange, 404, "Unknown type: " + type);
                return;
            }

            Path filePath = base.resolve(filename).normalize();
            if (!filePath.startsWith(base.normalize())) {
                sendText(exchange, 400, "Invalid path");
                return;
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                sendText(exchange, 404, "File not found");
                return;
            }

            byte[] data = Files.readAllBytes(filePath);
            String contentType = guessContentType(filename);
            sendBytes(exchange, 200, data, contentType);
        }

        private String joinFrom(String[] parts, int idx) {
            StringBuilder sb = new StringBuilder();
            for (int i = idx; i < parts.length; i++) {
                if (i > idx) sb.append("/");
                sb.append(parts[i]);
            }
            return sb.toString();
        }

        private String decodeUrl(String s) {
            try {
                return URLDecoder.decode(s, "UTF-8");
            } catch (Exception e) {
                return s;
            }
        }
    }

    private static class JsonBytesHandler implements HttpHandler {
        private final byte[] body;

        JsonBytesHandler(byte[] jsonBytes) {
            this.body = jsonBytes;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                sendNoContent(exchange);
                return;
            }
            if (!"GET".equalsIgnoreCase(method)) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            sendBytes(exchange, 200, body, "application/json; charset=utf-8");
        }
    }

    private static class TextHandler implements HttpHandler {
        private final byte[] body;

        TextHandler(String text) {
            this.body = text.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendBytes(exchange, 200, body, "text/plain; charset=utf-8");
        }
    }

    // -------- Helpers --------

    private static String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escapeJson(items.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static String guessContentType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private static void sendNoContent(HttpExchange exchange) throws IOException {
        addCors(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendBytes(exchange, status, json.getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        sendBytes(exchange, status, text.getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
    }

    private static void sendBytes(HttpExchange exchange, int status, byte[] body, String contentType) throws IOException {
        addCors(exchange);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        OutputStream os = exchange.getResponseBody();
        os.write(body);
        os.close();
    }

    private static void addCors(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String joinFrom(String[] parts, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < parts.length; i++) {
            if (i > startIndex) sb.append('/');
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}
