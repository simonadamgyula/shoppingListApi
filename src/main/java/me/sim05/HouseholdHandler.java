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

            if (url.equalsIgnoreCase("/household")) {
                handleGetHouseholds(exchange);
            } else
            if (url.equalsIgnoreCase("/household/get")) {
                handleGetHousehold(exchange);
            } else
            if (url.equalsIgnoreCase("/household/check_admin")) {
                handleCheckHouseholdAdmin(exchange);
            } else
            if (url.equalsIgnoreCase("/household/get_users")) {
                handleGetHouseholdUsers(exchange);
            } else
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
            } else
            if (url.equalsIgnoreCase("/household/set_permission")) {
                handleSetPermission(exchange);
            } else
            if (url.equalsIgnoreCase("/household/kick_member")) {
                handleKickMember(exchange);
            } else {
                HttpResponse.NotFound(exchange);
            }
        } catch (SQLException | ClassNotFoundException | IOException | RuntimeException e) {
            System.out.println(e.getMessage());
            HttpResponse.BadRequest(exchange, e.getMessage().getBytes());
        }
    }

    private void handleGetHouseholds(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        JSONObject[] households = database.getHouseholds(accountId);

        HttpResponse response = new HttpResponse(200);
        response.body.put("households", households);
        response.send(exchange);
    }

    private void handleGetHousehold(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        Integer householdId = body.getInt("household_id");

        JSONObject household;
        try {
            household = database.getHousehold(householdId, accountId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse response = new HttpResponse(200);
        response.body = household;
        response.send(exchange);
    }

    private void handleGetHouseholdUsers(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        Integer householdId = body.getInt("household_id");

        JSONObject[] users;
        try {
            users = database.getHouseholdUsers(householdId, accountId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse response = new HttpResponse(200);
        response.body.put("users", users);
        response.send(exchange);
    }

    private void handleCheckHouseholdAdmin(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        Integer householdId = body.getInt("household_id");

        boolean isAdmin;
        try {
            isAdmin = database.householdAuthorized(householdId, accountId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse response = new HttpResponse(200);
        response.body.put("is_admin", isAdmin);
        response.send(exchange);
    }

    private void handleUpdateHousehold(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        Integer householdId = body.getInt("household_id");
        String newName = body.getString("new_name");
        int newColor = body.getInt("new_color");
        UUID accountId = Authentication.getId(sessionId);

        try {
            database.updateHousehold(householdId, newName, newColor, accountId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse.OK(exchange, "Household updated".getBytes());
    }

    private void handleNewHousehold(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        String name = body.getString("name");
        int color = body.getInt("color");
        UUID accountId = Authentication.getId(sessionId);

        database.createHousehold(name, color, accountId);

        HttpResponse.OK(exchange, "Household created".getBytes());
    }

    private void handleDeleteHousehold(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        Integer householdId = body.getInt("household_id");
        UUID accountId = Authentication.getId(sessionId);

        try {
            database.deleteHousehold(householdId, accountId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse.OK(exchange, "Household deleted".getBytes());
    }

    private void handleNewHouseholdCode(HttpExchange exchange) throws SQLException, ClassNotFoundException, IOException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        Integer householdId = body.getInt("household_id");
        UUID accountId = Authentication.getId(sessionId);

        String code = database.createHouseholdCode(accountId, householdId);

        HttpResponse response = new HttpResponse(200);
        response.body.put("code", code);
        response.send(exchange);
    }

    private void handleJoinHousehold(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        String householdCode = body.getString("household_code");
        UUID accountId = Authentication.getId(sessionId);

        try {
            database.joinHousehold(accountId, householdCode);
        } catch (RuntimeException e) {
            HttpResponse.BadRequest(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse.OK(exchange, "Joined household".getBytes());
    }

    private void handleLeaveHousehold(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        Integer householdId = body.getInt("household_id");

        database.leaveHousehold(accountId, householdId);

        HttpResponse.OK(exchange, "Left household".getBytes());
    }

    private void handleSetPermission(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        UUID accountId = Authentication.getId(sessionId);
        Integer householdId = body.getInt("household_id");
        UUID userId = UUID.fromString(body.getString("user_id"));
        String permission = body.getString("permission");

        try {
            database.setMemberPermission(householdId, accountId, permission, userId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse.OK(exchange, "Permission set".getBytes());
    }

    private void handleKickMember(HttpExchange exchange) throws IOException, SQLException, ClassNotFoundException {
        String sessionId = body.getString("session_id");
        if (!Authentication.isAuthenticated(sessionId)) {
            HttpResponse.Unauthorized(exchange, "Unauthorized".getBytes());
            return;
        }

        Integer householdId = body.getInt("household_id");
        UUID accountId = Authentication.getId(sessionId);
        UUID userId = UUID.fromString(body.getString("user_id"));

        try {
            database.kickMember(householdId, accountId, userId);
        } catch (RuntimeException e) {
            HttpResponse.Forbidden(exchange, e.getMessage().getBytes());
            return;
        }

        HttpResponse.OK(exchange, "Member kicked".getBytes());
    }
}
