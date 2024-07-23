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
                r.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.sendResponseHeaders(500, -1);
        }
    }

    private void handlePut(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject jo;
        String name;
        String actorId;
        int statusCode;
        String response = "";

        try {
            jo = new JSONObject(body);
            name = jo.getString("name");
            actorId = jo.getString("actorId");
        } catch (JSONException e) {
            statusCode = 400;
            response = "Bad Request: Malformed JSON or missing required fields";
            sendResponse(r, statusCode, response);
            return;
        }

        try (Session session = Utils.driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                // Check if actor already exists
                StatementResult result = tx.run("MATCH (a:actor {id: $id}) RETURN a",
                        Values.parameters("id", actorId));

                if (result.hasNext()) {
                    statusCode = 400;
                    response = "Bad Request: Actor already exists";
                } else {
                    // Add the actor
                    tx.run("CREATE (:actor {id: $id, name: $name})",
                            Values.parameters("id", actorId, "name", name));
                    tx.success();
                    statusCode = 200;
                    response = "OK: Actor added successfully";
                }
            } catch (Exception e) {
                statusCode = 500;
                response = "Internal Server Error: " + e.getMessage();
            }
        } catch (Exception e) {
            statusCode = 500;
            response = "Internal Server Error: Database connection failed";
        }

        sendResponse(r, statusCode, response);
    }

    private void sendResponse(HttpExchange r, int statusCode, String response) throws IOException {
        r.sendResponseHeaders(statusCode, response.length());
//        OutputStream os = r.getResponseBody();
//        os.write(response.getBytes());
//        os.close();
    }
}