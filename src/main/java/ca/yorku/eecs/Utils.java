package ca.yorku.eecs;

import java.io.*;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import org.neo4j.driver.v1.*;

public class Utils {
    public static String uriDb = "bolt://localhost:7687";
    public static String uriUser ="http://localhost:8080";
    public static Driver driver;

    static {
        try {
            driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12345678"), Config.builder().withoutEncryption().build());
            // Test the connection
            try (Session session = driver.session()) {
                session.run("RETURN 1");
            }
        } catch (Exception e) {
            System.err.println("Failed to create the driver: " + e.getMessage());
            //  Exit the program here as the database is not accessible
            System.exit(1);
        }
    }

    public static String convert(InputStream inputStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public static void sendResponse(HttpExchange r, int statusCode, String response) throws IOException {
        r.sendResponseHeaders(statusCode, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
