package me.sim05;

import com.sun.net.httpserver.HttpExchange;
import org.json.Cookie;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

public class Utils {
    public static UUID masterId = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static JSONObject getBody(InputStream inputStream) throws IOException {
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder stringBuilder = new StringBuilder();
        Reader inputReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = inputReader.read(buffer, 0, buffer.length)) > 0; ) {
            stringBuilder.append(buffer, 0, numRead);
        }
        return new JSONObject(stringBuilder.toString());
    }

    public static JSONObject getCookies(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) {
            return null;
        }
        return Cookie.toJSONObject(cookie);
    }

    public static String getCookie(JSONObject cookies, String key) {
        if (cookies.getString("name").equals(key)) {
            return cookies.getString("value");
        }
        return cookies.getString(key);
    }

    public static String randomString(Integer size) {
        int leftLimit = 48;
        int rightLimit = 122;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(size)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static JSONObject getQueryParams(String query) {
        String[] params = query.split("&");
        JSONObject queryParams = new JSONObject();
        for (String param : params) {
            String[] keyValue = param.split("=");
            queryParams.put(keyValue[0], keyValue[1]);
        }
        return queryParams;
    }
}
