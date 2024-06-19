package me.sim05;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

public class HttpResponse {
    int responseCode;
    JSONObject body;

    public HttpResponse(int responseCode) {
        this.responseCode = responseCode;
        body = new JSONObject();
    }

    public void send(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json");

        exchange.sendResponseHeaders(responseCode, body.toString().length());
        OutputStream os = exchange.getResponseBody();
        os.write(body.toString().getBytes());
        os.close();
    }

    public static void OK(HttpExchange exchange, byte[] message) throws IOException {
        HttpResponse response = new HttpResponse(200);
        response.body.put("message", new String(message));
        response.send(exchange);
    }

    public static void BadRequest(HttpExchange exchange, byte[] message) throws IOException {
        HttpResponse response = new HttpResponse(400);
        response.body.put("message", new String(message));
        response.send(exchange);
    }

    public static void NotFound(HttpExchange exchange) throws IOException {
        HttpResponse response = new HttpResponse(404);
        response.body.put("message", "Not Found");
        response.send(exchange);
    }

    public static void Unauthorized(HttpExchange exchange, byte[] message) throws IOException {
        HttpResponse response = new HttpResponse(401);
        response.body.put("message", new String(message));
        response.send(exchange);
    }

    public static void Forbidden(HttpExchange exchange, byte[] message) throws IOException {
        HttpResponse response = new HttpResponse(403);
        response.body.put("message", new String(message));
        response.send(exchange);
    }
}
