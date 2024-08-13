package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * Main API handler that routes requests to specific endpoint handlers based on the request path.
 */
public class ApiHandler implements HttpHandler {

    /**
     * Handles the HTTP request by routing it to the appropriate endpoint handler.
     *
     * @param exchange The HttpExchange object representing the request and response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Remove the "/api/v1" prefix from the path
        String endpoint = path.substring("/api/v1".length());

        try {
            switch (endpoint) {
                case "/addActor":
                    new AddActorHandler().handle(exchange);
                    break;
                case "/addMovie":
                    new AddMovieHandler().handle(exchange);
                    break;
                case "/addRelationship":
                    new AddRelationshipHandler().handle(exchange);
                    break;
                case "/getActor":
                    new GetActorHandler().handle(exchange);
                    break;
                case "/getMovie":
                    new GetMovieHandler().handle(exchange);
                    break;
                case "/hasRelationship":
                    new HasRelationshipHandler().handle(exchange);
                    break;
                case "/computeBaconNumber":
                    new ComputeBaconNumberHandler().handle(exchange);
                    break;
                case "/computeBaconPath":
                    new ComputeBaconPathHandler().handle(exchange);
                    break;
                case "/addMovieRating":
                    new AddMovieRatingHandler().handle(exchange);
                    break;
                case "/getMoviesByRating":
                    new GetMoviesByRatingHandler().handle(exchange);
                    break;
                case "/getConnectStats":
                    new GetConnectStatsHandler().handle(exchange);
                    break;
                case "/getActorNetwork":
                    new GetActorNetworkHandler().handle(exchange);
                    break;
                default:
                    Utils.sendResponse(exchange, 404, "Not Found: " + endpoint);
            }
        } catch (Exception e) {
            Utils.sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}