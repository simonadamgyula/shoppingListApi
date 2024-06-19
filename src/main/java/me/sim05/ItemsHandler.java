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
        String url = exchange.getRequestURI().getPath();
        try {
            if (url.equalsIgnoreCase("/household/items")) {
                handleGetItems(exchange);
            } else {
                HttpResponse.NotFound(exchange);
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            HttpResponse.BadRequest(exchange, e.getMessage().getBytes());
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String url = exchange.getRequestURI().getPath();
        System.out.println(url);
        try {
            body = Utils.getBody(exchange.getRequestBody());

            if (url.equalsIgnoreCase("/household/items/add")) {
                handleAddItem(exchange);
            } else
            if (url.equalsIgnoreCase("/household/items/edit")) {
                handleEditItem(exchange);
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
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        JSONObject params = Utils.getQueryParams(exchange.getRequestURI().getQuery());

        UUID accountId = Authentication.getId(exchange);
        Integer householdId = params.getInt("household");

        JSONObject[] items = database.getItems(householdId, accountId);

        HttpResponse.OK(exchange, Arrays.toString(items).getBytes());
    }

    private void handleAddItem(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(exchange);
        Integer householdId = body.getInt("household_id");
        JSONObject item = body.getJSONObject("item");

        database.addItem(householdId, item, accountId);

        HttpResponse.OK(exchange, "Item added".getBytes());
    }

    private void handleEditItem(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(exchange);
        Integer householdId = body.getInt("household_id");
        JSONObject item = body.getJSONObject("item");

        database.editItem(householdId, item, accountId);

        HttpResponse.OK(exchange, "Item edited".getBytes());
    }

    private void handleRemoveItem(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(exchange);
        Integer householdId = body.getInt("household_id");
        String itemName = body.getString("item");

        database.removeItem(householdId, itemName, accountId);

        HttpResponse.OK(exchange, "Item removed".getBytes());
    }

}
