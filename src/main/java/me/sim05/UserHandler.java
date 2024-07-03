package me.sim05;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.UUID;

public class UserHandler implements HttpHandler {
    Database database;
    JSONObject body;

    public UserHandler() throws SQLException, ClassNotFoundException {
        database = Database.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("post")) {
            handlePost(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("get")) {
            handleGet(exchange);
        }
    }

    void handleGet(HttpExchange exchange) throws IOException {
        HttpResponse.NotFound(exchange);
    }

    void handlePost(HttpExchange exchange) throws IOException {
        String url = exchange.getRequestURI().getPath();
        System.out.println(url);
        try {
            body = Utils.getBody(exchange.getRequestBody());

            if (url.equalsIgnoreCase("/user/new")) {
                handleNewUser(exchange);
            } else
            if (url.equalsIgnoreCase("/user/edit")) {
                handleEditUser(exchange);
            } else
            if (url.equalsIgnoreCase("/user/check")) {
                handleUsernameCheck(exchange);
            } else
            if (url.equalsIgnoreCase("/user/delete")) {
                handleDeleteUser(exchange);
            } else
            if (url.equalsIgnoreCase("/user/authenticate")) {
                handleAuthentication(exchange);
            } else
            if (url.equalsIgnoreCase("/user/get_user")) {
                handleGetUser(exchange);
            } else {
                HttpResponse.NotFound(exchange);
            }
        } catch (SQLException | ClassNotFoundException | IOException | NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
            HttpResponse.BadRequest(exchange, e.getMessage().getBytes());
        }
    }

    private void handleGetUser(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID user_id = Authentication.getId(sessionId);
        System.out.println(user_id);
        JSONObject user = database.getUser(user_id);

        if (user == null) {
            HttpResponse.BadRequest(exchange, "User not found".getBytes());
            return;
        }

        HttpResponse response = new HttpResponse(200);
        response.body = user;
        response.send(exchange);
    }

    void handleEditUser(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID user_id = Authentication.getId(sessionId);
        String username = body.getString("username");
        String profile_picture = body.getString("profile_picture");

        Integer result = database.editUser(user_id, username, profile_picture);

        if (result == 0) {
            HttpResponse.BadRequest(exchange, "User not found".getBytes());
        } else {
            HttpResponse.OK(exchange, "User edited".getBytes());
        }
    }

    void handleUsernameCheck(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String username = body.getString("username");
        Database database = Database.getInstance();

        if (database.usernameExists(username)) {
            HttpResponse.OK(exchange, "Username exists".getBytes());
        } else {
            HttpResponse.OK(exchange, "Username does not exist".getBytes());
        }
    }

    void handleNewUser(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException, NoSuchAlgorithmException {
        if (!body.has("username") || !body.has("password")) {
            HttpResponse.BadRequest(exchange, "Missing username or password".getBytes());
            return;
        }

        String username = body.getString("username");
        String password = body.getString("password");
        String profile_picture = body.getString("profile_picture");

        Hash hash = new Hash(password);
        hash.generateSalt();
        hash.hashPassword();

        String passwordToStore = hash.getSalt() + ":" + hash.getHashedPassword();

        Database database = Database.getInstance();
        try {
            database.insertUser(username, passwordToStore, profile_picture);
        } catch (RuntimeException e) {
            HttpResponse.BadRequest(exchange, e.getMessage().getBytes());
            return;
        }

        UUID session_id = Authentication.authenticate(username, password);
        if (session_id == null) {
            HttpResponse.BadRequest(exchange, "Couldn't authenticate, try logging in manually".getBytes());
            return;
        }
        HttpResponse.OK(exchange, session_id.toString().getBytes());
    }

    void handleDeleteUser(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        Database database = Database.getInstance();

        String sessionId = body.getString("session_id");
        UUID user_id = Authentication.getId(sessionId);

        Integer result = database.deleteUser(user_id);

        if (result == 0) {
            HttpResponse.BadRequest(exchange, "User not found".getBytes());
        } else {
            HttpResponse.OK(exchange, "User deleted".getBytes());
        }
    }

    void handleAuthentication(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String username = body.getString("username");
        String password = body.getString("password");

        UUID sessionId = Authentication.authenticate(username, password);
        if (sessionId == null) {
            HttpResponse.BadRequest(exchange, "Invalid username or password".getBytes());
        } else {
            HttpResponse response = new HttpResponse(200);
            response.body.put("session_id", sessionId.toString());
            response.send(exchange);
        }
    }
}
