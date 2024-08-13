package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;

import java.io.IOException;

/**
 * Handles HTTP PUT requests to add or update a movie rating in the database.
 * This handler checks if the movie exists and adds/updates its rating.
 */
public class AddMovieRatingHandler implements HttpHandler {

    /**
     * Handles the HTTP request.
     *
     * @param r The HttpExchange object representing the request and response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("PUT")) {
                handlePut(r);
            } else {
                Utils.sendResponse(r, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(r, 500, "Internal Server Error");
        }
    }

    /**
     * Processes the PUT request to add or update a movie rating.
     *
     * @param r The HttpExchange object representing the request and response.
     * @throws IOException If an I/O error occurs.
     * @throws JSONException If there's an error parsing the JSON request body.
     */
    private void handlePut(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject jo;
        String movieId;
        float rating;

        try {
            jo = new JSONObject(body);
            movieId = jo.getString("movieId");
            rating = (float) jo.getDouble("rating");

            if (rating < 0.0 || rating > 10.0) {
                Utils.sendResponse(r, 400, "Bad Request: Rating must be between 0.0 and 10.0");
                return;
            }
        } catch (JSONException e) {
            Utils.sendResponse(r, 400, "Bad Request: Malformed JSON or missing required fields");
            return;
        }

        try (Session session = Utils.driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                StatementResult result = tx.run("MATCH (m:movie {id: $id}) RETURN m",
                        Values.parameters("id", movieId));

                if (!result.hasNext()) {
                    Utils.sendResponse(r, 404, "Not Found: Movie does not exist");
                    return;
                }

                // Store up to 1 decimal place
                tx.run("MATCH (m:movie {id: $id}) SET m.rating = ROUND($rating * 10) / 10.0",
                        Values.parameters("id", movieId, "rating", rating));
                tx.success();
                Utils.sendResponse(r, 200, "OK: Movie rating added/updated successfully");
            }
        } catch (Exception e) {
            Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}

