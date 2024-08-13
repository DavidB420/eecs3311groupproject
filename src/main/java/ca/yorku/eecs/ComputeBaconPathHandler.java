package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Handles HTTP GET requests to compute the Bacon path for a given actor.
 * The Bacon path is the shortest path of actor-movie connections from the given actor to Kevin Bacon.
 */
public class ComputeBaconPathHandler implements HttpHandler {
    private static final String KEVIN_BACON_ID = "nm0000102";

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
     * Processes the GET request to compute the Bacon path for a given actor.
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
            jo = new JSONObject(body);
            actorId = jo.getString("actorId");
        } catch (JSONException e) {
            Utils.sendResponse(r, 400, "Bad Request: Malformed JSON or missing required fields");
            return;
        }

        try (Session session = Utils.driver.session()) {
            List<String> baconPath = computeBaconPath(session, actorId);
            
            if (baconPath != null) {
                JSONObject response = new JSONObject();
                response.put("baconPath", new JSONArray(baconPath));
                Utils.sendResponse(r, 200, response.toString());
            } else {
                Utils.sendResponse(r, 404, "Not Found: Actor does not exist or has no path to Kevin Bacon");
            }
        } catch (Exception e) {
            Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Computes the Bacon path for a given actor using the Neo4j database.
     *
     * @param session The Neo4j database session.
     * @param actorId The ID of the actor for whom to compute the Bacon path.
     * @return A list of actor and movie IDs representing the Bacon path, or null if no path exists.
     */
    protected static List<String> computeBaconPath(Session session, String actorId) {
        if (actorId.equals(KEVIN_BACON_ID)) {
            return Collections.singletonList(KEVIN_BACON_ID);
        }

        String query = 
            "MATCH (start:actor {id: $startId}), (bacon:actor {id: $baconId}) " +
            "MATCH p = shortestPath((start)-[:ACTED_IN*]-(bacon)) " +
            "RETURN [node in nodes(p) | node.id] as path";

        StatementResult result = session.run(query, 
            Values.parameters("startId", actorId, "baconId", KEVIN_BACON_ID));

        if (result.hasNext()) {
            return result.next().get("path").asList(Value::asString);
        }
        return null; // No path found
    }
}