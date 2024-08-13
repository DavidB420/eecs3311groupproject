package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;

import java.io.IOException;

/**
 * Handles HTTP GET requests to retrieve movies within a specified rating range from the database.
 * This handler fetches and returns a list of movies sorted by their ratings in descending order.
 */
public class GetMoviesByRatingHandler implements HttpHandler {

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
                // Method not allowed for non-GET requests
                Utils.sendResponse(r, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(r, 500, "Internal Server Error");
        }
    }

    /**
     * Processes the GET request to retrieve movies within a specified rating range.
     *
     * @param r The HttpExchange object representing the request and response.
     * @throws IOException If an I/O error occurs.
     * @throws JSONException If there's an error parsing the JSON request body or creating the JSON response.
     */
    private void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject jo;
        float minRating, maxRating;

        try {
            // Parse JSON and extract required fields
            jo = new JSONObject(body);
            minRating = (float) jo.getDouble("minRating");
            maxRating = (float) jo.getDouble("maxRating");

            if (minRating < 0.0 || minRating > 10.0) {
                Utils.sendResponse(r, 400, "Bad Request: minRating must be between 0.0 and 10.0");
                return;
            }

            if (maxRating < 0.0 || maxRating > 10.0) {
                Utils.sendResponse(r, 400, "Bad Request: maxRating must be between 0.0 and 10.0");
                return;
            }

            if (minRating > maxRating) {
                Utils.sendResponse(r, 400, "Bad Request: minRating must be less than or equal to maxRating ");
                return;
            }

        } catch (JSONException e) {
            Utils.sendResponse(r, 400, "Bad Request: Malformed JSON or missing required fields");
            return;
        }

        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (m:movie) WHERE m.rating >= $minRating AND m.rating <= $maxRating " +
                            "RETURN m.id as movieId, m.name as name, m.rating as rating " +
                            "ORDER BY m.rating DESC",
                    Values.parameters("minRating", minRating, "maxRating", maxRating)
            );

            JSONObject response = new JSONObject();
            JSONArray movies = new JSONArray();

            while (result.hasNext()) {
                Record record = result.next();
                JSONObject movie = new JSONObject();
                movie.put("movieId", record.get("movieId").asString());
                movie.put("name", record.get("name").asString());
                movie.put("rating", record.get("rating").asFloat());
                movies.put(movie);
            }

            response.put("movies", movies);
            Utils.sendResponse(r, 200, response.toString());
        } catch (Exception e) {
            Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}