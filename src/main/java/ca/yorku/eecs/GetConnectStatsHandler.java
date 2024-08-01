package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetConnectStatsHandler implements HttpHandler {

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