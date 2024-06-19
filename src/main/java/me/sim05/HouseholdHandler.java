package me.sim05;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class HouseholdHandler implements HttpHandler {
    Database database;
    JSONObject body;

    public HouseholdHandler() throws SQLException, ClassNotFoundException {
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

            if (url.equalsIgnoreCase("/household/new")) {
                handleNewHousehold(exchange);
            } else
            if (url.equalsIgnoreCase("/household/delete")) {
                handleDeleteHousehold(exchange);
            } else
            if (url.equalsIgnoreCase("/household/update")) {
                handleUpdateHousehold(exchange);
            } else
            if (url.equalsIgnoreCase("/household/join")) {
                handleJoinHousehold(exchange);
            } else
            if (url.equalsIgnoreCase("/household/new_code")) {
                handleNewHouseholdCode(exchange);
            } else
            if (url.equalsIgnoreCase("/household/leave")) {
                handleLeaveHousehold(exchange);
            } else {
                HttpResponse.NotFound(exchange);
            }
        } catch (SQLException | ClassNotFoundException | IOException | RuntimeException e) {
            System.out.println(e.getMessage());
            HttpResponse.BadRequest(exchange, e.getMessage().getBytes());
        }
    }

    private void handleUpdateHousehold(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        Integer householdId = body.getInt("household_id");
        String newName = body.getString("new_name");
        UUID accountId = Authentication.getId(exchange);

        try {
            database.updateHousehold(householdId, newName, accountId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse.OK(exchange, "Household updated".getBytes());
    }

    private void handleNewHousehold(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        String name = body.getString("name");
        UUID accountId = Authentication.getId(exchange);

        database.createHousehold(name, accountId);

        HttpResponse.OK(exchange, "Household created".getBytes());
    }

    private void handleDeleteHousehold(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        Integer householdId = body.getInt("household_id");
        UUID accountId = Authentication.getId(exchange);

        try {
            database.deleteHousehold(householdId, accountId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse.OK(exchange, "Household deleted".getBytes());
    }

    private void handleNewHouseholdCode(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        Integer householdId = body.getInt("household_id");
        UUID accountId = Authentication.getId(exchange);

        String code = database.createHouseholdCode(accountId, householdId);

        HttpResponse response = new HttpResponse(200);
        response.body.put("code", code);
        response.send(exchange);
    }

    private void handleJoinHousehold(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        String householdCode = body.getString("household_code");
        UUID accountId = Authentication.getId(exchange);

        database.joinHousehold(accountId, householdCode);

        HttpResponse.OK(exchange, "Joined household".getBytes());
    }

    private void handleLeaveHousehold(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        if (!Authentication.isAuthenticated(exchange)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(exchange);
        Integer householdId = body.getInt("household_id");

        database.leaveHousehold(accountId, householdId);
    }
}
