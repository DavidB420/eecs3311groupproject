package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles HTTP GET requests to retrieve statistics about actor connections.
 * This handler can return either the most or least connected actors, up to a specified limit.
 */
public class GetConnectStatsHandler implements HttpHandler {

    /**
     * Handles the HTTP request.
     *
     * @param r The HttpExchange object representing the request and response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else {
                Utils.sendResponse(r, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(r, 500, "Internal Server Error");
        }
    }

    /**
     * Processes the GET request to retrieve actor connection statistics.
     *
     * @param r The HttpExchange object representing the request and response.
     * @throws IOException If an I/O error occurs.
     * @throws JSONException If there's an error parsing the JSON request body or creating the JSON response.
     */
    private void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject jo = new JSONObject(body);

        int limit = jo.optInt("limit", 5);
        boolean most = jo.optBoolean("most", true);

        if (limit < 0) {
            Utils.sendResponse(r, 400, "Bad Request: Limit cannot be negative");
            return;
        }

        try (Session session = Utils.driver.session()) {
            List<JSONObject> actors = getConnectStats(session, limit, most);

            JSONObject response = new JSONObject();
            response.put("actors", new JSONArray(actors));
            Utils.sendResponse(r, 200, response.toString());
        } catch (Exception e) {
            Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Retrieves connection statistics for actors from the database.
     *
     * @param session The Neo4j database session.
     * @param limit The maximum number of actors to return.
     * @param most If true, return the most connected actors; if false, return the least connected actors.
     * @return A list of JSONObjects containing actor IDs and their connection counts.
     * @throws JSONException If there's an error creating the JSON objects.
     */
    private List<JSONObject> getConnectStats(Session session, int limit, boolean most) throws JSONException {
        String query =
                        "MATCH (a:actor) " +
                        "OPTIONAL MATCH (a)-[:ACTED_IN]->(m:movie) " +
                        "WITH a, COUNT(DISTINCT m) as connections " +
                        "RETURN a.id as actorId, connections " +
                        "ORDER BY connections " + (most ? "DESC" : "ASC") + " " +
                        "LIMIT $limit";

        StatementResult result = session.run(query, Values.parameters("limit", limit));

        List<JSONObject> actors = new ArrayList<>();
        while (result.hasNext()) {
            Record record = result.next();
            JSONObject actor = new JSONObject();
            actor.put("actorId", record.get("actorId").asString());
            actor.put("connections", record.get("connections").asInt());
            actors.add(actor);
        }

        return actors;
    }
}