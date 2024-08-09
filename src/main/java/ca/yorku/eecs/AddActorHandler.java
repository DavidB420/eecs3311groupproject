package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;

import java.io.IOException;
import java.io.OutputStream;

public class AddActorHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("PUT")) {
                handlePut(r);
            } else {
                // Method not allowed for non-PUT requests
                Utils.sendResponse(r, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(r, 500, "Internal Server Error");
        }
    }

    private void handlePut(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject jo;
        String name;
        String actorId;

        try {
            // Parse JSON and extract required fields
            jo = new JSONObject(body);
            name = jo.getString("name");
            actorId = jo.getString("actorId");
        } catch (JSONException e) {
            Utils.sendResponse(r, 400, "Bad Request: Malformed JSON or missing required fields");
            return;
        }

        try (Session session = Utils.driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                // Check if actor already exists
                StatementResult result = tx.run("MATCH (a:actor {id: $id}) RETURN a",
                        Values.parameters("id", actorId));

                if (result.hasNext()) {
                    Utils.sendResponse(r, 400, "Bad Request: Actor already exists");
                } else {
                    // Add the actor
                    tx.run("CREATE (:actor {id: $id, name: $name})",
                            Values.parameters("id", actorId, "name", name));
                    tx.success();
                    Utils.sendResponse(r, 200, "OK: Actor added successfully");
                }
            }
        } catch (Exception e) {
            Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}
