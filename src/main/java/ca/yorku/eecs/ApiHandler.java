package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class ApiHandler implements HttpHandler {
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
                    notFound(exchange);
            }
        } catch (Exception e) {
            internalServerError(exchange, e);
        }
    }

    private void notFound(HttpExchange exchange) throws IOException {
        String response = "Not Found";
        exchange.sendResponseHeaders(404, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void internalServerError(HttpExchange exchange, Exception e) throws IOException {
        String response = "Internal Server Error: " + e.getMessage();
        exchange.sendResponseHeaders(500, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}