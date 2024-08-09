package ca.yorku.eecs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class GetActorNetworkHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange r) throws IOException {
		// TODO Auto-generated method stub
		try {
			if (r.getRequestMethod().equals("GET")) {
				handleGet(r);
			} else {
				// Method not allowed for non-GET requests
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
            // Parse JSON and extract required fields
            jo = new JSONObject(body);
            actorId = jo.getString("actorId");
        } catch (JSONException e) {
            Utils.sendResponse(r, 400, "Bad Request: Malformed JSON or missing required fields");
            return;
        }

        try (Session session = Utils.driver.session()) {
            // Start a transaction
            try (Transaction tx = session.beginTransaction()) {
                // Check if actor exists
                StatementResult existsResult = tx.run(
                        "MATCH (a:actor {id: $id}) RETURN a",
                        Values.parameters("id", actorId)
                );

                if (!existsResult.hasNext()) {
                    tx.success();  // Mark the transaction as successful (though no changes were made)
                    tx.close();
                    Utils.sendResponse(r, 404, "Not Found: Actor does not exist");
                    return;
                }

                // Get actor's network
                StatementResult result = tx.run(
                        "MATCH (a:actor {id: $id}) " +
                                "OPTIONAL MATCH (a)-[:ACTED_IN]->(m:movie)<-[:ACTED_IN]-(ca:actor) " +
                                "WHERE a.id <> ca.id " +
                                "RETURN DISTINCT ca.id as coActorId",
                        Values.parameters("id", actorId)
                );

                List<String> actors = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    String coActorId = record.get("coActorId").asString(null);
                    if (coActorId != null) {
                        actors.add(coActorId);
                    }
                }

                JSONObject response = new JSONObject();
                JSONArray actorsArray = new JSONArray(actors);
                response.put("actors", actorsArray);

                tx.success();  // Mark the transaction as successful
                tx.close();  // Commit the transaction
                Utils.sendResponse(r, 200, response.toString());
            } catch (Exception e) {
                // If an error occurs, the transaction is automatically rolled back
                e.printStackTrace();
                Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}