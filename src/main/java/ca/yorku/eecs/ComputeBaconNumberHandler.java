package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles HTTP GET requests to compute the Bacon number for a given actor.
 * The Bacon number represents the degree of separation between an actor and Kevin Bacon.
 */
public class ComputeBaconNumberHandler implements HttpHandler {
    private static final String KEVIN_BACON_ID = "nm0000102";

    /**
     * Handles the HTTP request.
     *
     * @param exchange The HttpExchange object representing the request and response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (exchange.getRequestMethod().equals("GET")) {
                handleGet(exchange);
            } else {
                // Method not allowed for non-GET requests
                Utils.sendResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    /**
     * Processes the GET request to compute the Bacon number for a given actor.
     *
     * @param r The HttpExchange object representing the request and response.
     * @throws IOException If an I/O error occurs.
     * @throws JSONException If there's an error parsing the JSON request body or creating the JSON response.
     */
    private void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject jo;
        String actorId;

        try {
            // Parse JSON and extract required fields
            jo = new JSONObject(body);
            actorId = jo.getString("actorId");
        } catch (JSONException e) {
            Utils.sendResponse(r, 400, "Bad Request: Malformed JSON or missing required fields");
            return;
        }

        try (Session session = Utils.driver.session()) {
            int count = 0;
            List<String> baconPath = ComputeBaconPathHandler.computeBaconPath(session, actorId);
            
            if (baconPath != null) {
                JSONObject response = new JSONObject();

                for (int i = 0; i < baconPath.size(); i++){
                    if (i % 2 == 1) count++;
                }

                response.put("baconNumber", count);
                Utils.sendResponse(r, 200, response.toString());
            } else {
                Utils.sendResponse(r, 404, "Not Found: Actor does not exist or has no path to Kevin Bacon");
            }
        } catch (Exception e) {
            Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}