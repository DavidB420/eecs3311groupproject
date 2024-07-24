package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        // Create contexts for each endpoint
        server.createContext("/api/v1/addActor", new AddActorHandler());
        server.createContext("/api/v1/addMovie", new AddMovieHandler());
        server.createContext("/api/v1/addRelationship", new AddRelationshipHandler());
        server.createContext("/api/v1/getActor", new GetActorHandler());
        server.createContext("/api/v1/getMovie", new GetMovieHandler());
        server.createContext("/api/v1/hasRelationship", new HasRelationshipHandler());
        server.createContext("/api/v1/computeBaconNumber", new ComputeBaconNumberHandler());
        server.createContext("/api/v1/computeBaconPath", new ComputeBaconPathHandler());

        // New feature endpoints
        server.createContext("/api/v1/addMovieRating", new AddMovieRatingHandler());
        server.createContext("/api/v1/getMoviesByRating", new GetMoviesByRatingHandler());

        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}