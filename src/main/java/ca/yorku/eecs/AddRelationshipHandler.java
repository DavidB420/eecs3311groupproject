package ca.yorku.eecs;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Handles HTTP PUT requests to add a relationship between an actor and a movie in the database.
 * This handler checks if both the actor and movie exist, and if the relationship doesn't already exist.
 */
public class AddRelationshipHandler implements HttpHandler {

	private final String relationship = "ACTED_IN";

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
	 * Processes the PUT request to add a relationship between an actor and a movie.
	 *
	 * @param r The HttpExchange object representing the request and response.
	 * @throws IOException If an I/O error occurs.
	 * @throws JSONException If there's an error parsing the JSON request body.
	 */
	private void handlePut(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject jo;
		String actorId;
		String movieId;

		try {
			jo = new JSONObject(body);
			actorId = jo.getString("actorId");
			movieId = jo.getString("movieId");
		} catch (JSONException e) {
			Utils.sendResponse(r, 400, "Bad Request: Malformed JSON or missing required fields");
			return;
		}

		try (Session session = Utils.driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				// Check if actor and/or movie does not exist
				StatementResult resultActor = tx.run("MATCH (a:actor {id: $id}) RETURN a",
						Values.parameters("id", actorId));
				
				StatementResult resultMovie = tx.run("MATCH (m:movie {id: $id}) RETURN m",
						Values.parameters("id", movieId));

				if (!resultActor.hasNext() || !resultMovie.hasNext()) {
					Utils.sendResponse(r, 404, "Not Found: Actor and/or movie does not exist");
					return;
				}

				//Check if relationship exists
				StatementResult resultRelationship = tx.run(
						"MATCH (a:actor {id: $actorId})-[r:" + relationship + "]->(m:movie {id: $movieId}) RETURN r",
						Values.parameters("actorId", actorId, "movieId", movieId));

				if (resultRelationship.hasNext()) {
					Utils.sendResponse(r, 400, "Bad Request: Relationship already exists");
				} else {
					// Create the relationship between actor and movie
					tx.run("MATCH (a:actor {id: $actorId}), (m:movie {id: $movieId}) " +
							"CREATE (a)-[r:" + relationship + "]->(m)",
							Values.parameters("actorId", actorId, "movieId", movieId));

					tx.success();
					Utils.sendResponse(r, 200, "OK: Relationship added successfully");
				}

			}
		} catch (Exception e) {
			Utils.sendResponse(r, 500, "Internal Server Error: " + e.getMessage());
		}
	}
}
