package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.Record;

import java.io.IOException;
import java.io.OutputStream;

public class GetMovieHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange r) throws IOException {
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
        String movieId;

        try {
            // Parse JSON and extract required fields
            jo = new JSONObject(body);
            movieId = jo.getString("movieId");
        } catch (JSONException e) {
            Utils.sendResponse(r, 400, "Bad Request: Malformed JSON or missing required fields");
            return;
        }

        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (m:movie {id: $id}) " +
                            "OPTIONAL MATCH (a:actor)-[:ACTED_IN]->(m) " +
                            "RETURN m.name as name, collect(a.id) as actors",
                    Values.parameters("id", movieId)
            );

            if (result.hasNext()) {
                Record record = result.next();
                JSONObject response = new JSONObject();
                response.put("movieId", movieId);
                response.put("name", record.get("name").asString());
                response.put("actors", new JSONArray(record.get("actors").asList()));

                Utils.sendResponse(r, 200, response.toString());
            } else {
                Utils.sendResponse(r, 404, "Not Found: Movie does not exist");
            }
        } catch (Exception e) {
            Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
        }
    }


}
