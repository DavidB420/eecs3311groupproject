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

        try {
            switch (path) {
                case "/api/v1/addActor":
                    new AddActorHandler().handle(exchange);
                    break;
                case "/api/v1/addMovie":
                    new AddMovieHandler().handle(exchange);
                    break;
                case "/api/v1/addRelationship":
                    new AddRelationshipHandler().handle(exchange);
                    break;
                case "/api/v1/getActor":
                    new GetActorHandler().handle(exchange);
                    break;
                case "/api/v1/getMovie":
                    new GetMovieHandler().handle(exchange);
                    break;
                case "/api/v1/hasRelationship":
                    new HasRelationshipHandler().handle(exchange);
                    break;
                case "/api/v1/computeBaconNumber":
                    new ComputeBaconNumberHandler().handle(exchange);
                    break;
                case "/api/v1/computeBaconPath":
                    new ComputeBaconPathHandler().handle(exchange);
                    break;
                case "/api/v1/addMovieRating":
                    new AddMovieRatingHandler().handle(exchange);
                    break;
                case "/api/v1/getMoviesByRating":
                    new GetMoviesByRatingHandler().handle(exchange);
                    break;
                case "/api/v1/getConnectStats":
                    new GetConnectStatsHandler().handle(exchange);
                    break;
                case "/api/v1/getActorNetwork":
                    new GetActorNetworkHandler().handle(exchange);
                    break;
                default:
                    Utils.sendResponse(exchange, 404, "Not Found");
            }
        } catch (Exception e) {
            Utils.sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}