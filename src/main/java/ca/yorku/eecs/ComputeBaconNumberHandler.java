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

public class ComputeBaconNumberHandler implements HttpHandler {
    private static final String KEVIN_BACON_ID = "nm0000102";

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
            try (Transaction tx = session.beginTransaction()) {
                StatementResult resultActor = tx.run("MATCH (a:actor {id: $id}) RETURN a",
                Values.parameters("id", actorId));

                if (resultActor.hasNext()){
                
                    List<String> queue = new ArrayList<String>();
                    int currentDistance = 0;

                    //Add starting actor id to bfs queue
                    queue.add(actorId);

                    while (!queue.isEmpty()) {
                        StatementResult result = tx.run("MATCH (a:actor {id: $id})-[:ACTED_IN*1]->(fof) RETURN DISTINCT fof",  Values.parameters("id", actorId));
                        System.out.println(result.single().get("length"));    
                    }
                }

            }
        }
    }
}