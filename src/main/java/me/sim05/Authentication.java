package me.sim05;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.UUID;

public class Authentication {
    public static UUID getId(HttpExchange exchange) throws SQLException, ClassNotFoundException {
        String sessionId = getSessionId(exchange);
        if (sessionId == null) {
            return null;
        }
        Database database = Database.getInstance();

        return database.getAccountIdBySessionId(sessionId);
    }

    static String getSessionId(HttpExchange exchange) {
        JSONObject cookies = Utils.getCookies(exchange);
        if (cookies == null) {
            return null;
        }

        return Utils.getCookie(cookies, "session_id");
    }

    public static boolean isAuthenticated(HttpExchange exchange) throws SQLException, ClassNotFoundException {
        return getId(exchange) != null;
    }

    public static UUID authenticate(String username, String password, String ipAddress) throws SQLException, ClassNotFoundException {
        Database database = Database.getInstance();

        String storedPassword = database.getPassword(username);
        if (storedPassword == null) return null;

        String[] separated = Hash.separateSaltAndHash(storedPassword);
        String salt = separated[0];
        String hashedPassword = separated[1];

        try {
            if (!Hash.verify(password, salt, hashedPassword)) return null;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        UUID accountId = database.getAccountIdByUsername(username);

        UUID sessionId = database.getSession(accountId, ipAddress);
        if (sessionId != null) {
            return sessionId;
        }
        return database.createSession(accountId, ipAddress);
    }
}