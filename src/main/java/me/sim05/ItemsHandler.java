package me.sim05;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

public class ItemsHandler implements HttpHandler {
    Database database;
    JSONObject body;

    public ItemsHandler() throws SQLException, ClassNotFoundException {
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

    private void handleGet(HttpExchange exchange) throws IOException {
        HttpResponse.NotFound(exchange);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String url = exchange.getRequestURI().getPath();
        System.out.println(url);
        try {
            body = Utils.getBody(exchange.getRequestBody());

            if (url.equalsIgnoreCase("/household/items")) {
                handleGetItems(exchange);
            } else
            if (url.equalsIgnoreCase("/household/items/add")) {
                handleAddItem(exchange);
            } else
            if (url.equalsIgnoreCase("/household/items/edit")) {
                handleEditItem(exchange);
            } else
            if (url.equalsIgnoreCase("/household/items/set_bought")) {
                handleSetItemBought(exchange);
            } else
            if (url.equalsIgnoreCase("/household/items/remove")) {
                handleRemoveItem(exchange);
            } else {
                HttpResponse.NotFound(exchange);
            }

        } catch (SQLException | ClassNotFoundException | IOException e) {
            System.out.println(e.getMessage());
            HttpResponse.BadRequest(exchange, e.getMessage().getBytes());
        }
    }

    private void handleGetItems(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        Integer householdId = body.getInt("household");

        JSONObject[] items;
        try {
            items = database.getItems(householdId, accountId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse response = new HttpResponse(200);
        response.body.put("items", Arrays.toString(items));
        response.send(exchange);
    }

    private void handleAddItem(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        System.out.println("here");

        Integer householdId = body.getInt("household_id");
        UUID accountId = Authentication.getId(sessionId);
        JSONObject item = body.getJSONObject("item");
        System.out.println("here");

        database.addItem(householdId, item, accountId);
        System.out.println("here");

        HttpResponse.OK(exchange, "Item added".getBytes());
        System.out.println("here");
    }

    private void handleEditItem(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        Integer householdId = body.getInt("household_id");
        JSONObject item = body.getJSONObject("item");

        database.editItem(householdId, item, accountId);

        HttpResponse.OK(exchange, "Item edited".getBytes());
    }

    private void handleSetItemBought(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        int householdId = body.getInt("household_id");
        String itemName = body.getString("item");
        boolean bought = body.getBoolean("bought");

        System.out.println("householdId: " + householdId);

        database.setItemBought(householdId, itemName, accountId, bought);

        HttpResponse.OK(exchange, "Item bought".getBytes());
    }

    private void handleRemoveItem(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        Integer householdId = body.getInt("household_id");
        String itemName = body.getString("item");

        database.removeItem(householdId, itemName, accountId);

        HttpResponse.OK(exchange, "Item removed".getBytes());
    }

}
