package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

/**
 * Main application class that sets up and starts the HTTP server.
 */
public class App
{
    static int PORT = 8080;

    /**
     * The main method that initializes and starts the HTTP server.
     *
     * @param args Command line arguments (not used).
     * @throws IOException If an I/O error occurs while starting the server.
     */
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        // Create a single context for all endpoints
        server.createContext("/api/v1", new ApiHandler());

        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}