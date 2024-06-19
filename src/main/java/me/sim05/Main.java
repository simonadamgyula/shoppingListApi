package me.sim05;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
        try {
            server.createContext("/user", new UserHandler());
            server.createContext("/household", new HouseholdHandler());
            server.createContext("/household/items", new ItemsHandler());
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        server.start();
        System.out.println(" Server started on port 8001");
    }
}