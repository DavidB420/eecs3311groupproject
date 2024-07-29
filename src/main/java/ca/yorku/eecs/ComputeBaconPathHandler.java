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

public class ComputeBaconPathHandler implements HttpHandler {
    private static final String KEVIN_BACON_ID = "nm0000102";

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