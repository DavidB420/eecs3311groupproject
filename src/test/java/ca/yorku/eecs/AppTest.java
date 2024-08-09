package ca.yorku.eecs;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.Record;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase
{
    private static final String BASE_URL = "http://localhost:8080/api/v1/";

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Clear the database or set it to a known state before each test
        try (Session session = Utils.driver.session()) {
            System.out.println("Clearing the database...");
            session.run("MATCH (n) DETACH DELETE n");
            Thread.sleep(700); // Add a small delay
            System.out.println("Database cleared.");
        } catch (Exception e) {
            System.err.println("Error during setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends an HTTP request to a specified endpoint using the specified method and parameters.
     * Uses curl command to perform the request.
     *
     * @param endpoint The endpoint to which the request is sent.
     * @param method The HTTP method (GET, PUT, POST, DELETE, etc.) to use for the request.
     * @param params The JSON object containing the request parameters.
     * @return A JSON object containing the status code and response body.
     * @throws Exception If an error occurs during the request.
     */
    private JSONObject sendRequest(String endpoint, String method, JSONObject params) throws Exception {
        // Construct the full URL
        String url = BASE_URL + endpoint;

        // Build the curl command
        StringBuilder command = new StringBuilder("curl -X ").append(method.toUpperCase()).append(" ").append(url);

        // If there are parameters, add them to the command
        if (params != null && params.length() > 0) {
            String paramString = params.toString();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Windows: Escape double quotes and use double quotes around the JSON payload
                command.append(" -H \"Content-Type: application/json\" -d \"").append(paramString.replace("\"", "\\\"")).append("\"");
            } else {
                // Unix-like: Use single quotes around the JSON payload
                command.append(" -H 'Content-Type: application/json' -d '").append(paramString).append("'");
            }
        }

        // Add a command to write the HTTP status code to the output
        command.append(" -w \"\\n%{http_code}\"");

        // Print the constructed command for debugging
        System.out.println("Executing command: " + command.toString());

        // Determine the OS and execute the command accordingly
        ProcessBuilder processBuilder;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Use cmd.exe on Windows
            processBuilder = new ProcessBuilder("cmd.exe", "/c", command.toString());
        } else {
            // Use bash on Unix-like systems
            processBuilder = new ProcessBuilder("bash", "-c", command.toString());
        }
        
        Process process = processBuilder.start();

        // Read the output of the curl command
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String result = reader.lines().collect(Collectors.joining("\n"));

        // Wait for the process to complete and check the exit code
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Curl command failed with exit code " + exitCode);
        }

        // Parse the status code from the output
        int lastNewlineIndex = result.lastIndexOf('\n');
        int statusCode = Integer.parseInt(result.substring(lastNewlineIndex + 1).trim());

        // Get the response body
        String responseBody = result.substring(0, lastNewlineIndex).trim();
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("statusCode", statusCode);

        // If the response body is a valid JSON object, parse it; otherwise, treat it as a string
        if (!responseBody.isEmpty() && responseBody.startsWith("{")) {
            jsonResponse.put("body", new JSONObject(responseBody));
        } else {
            jsonResponse.put("body", responseBody);
        }

        // Print the status code and response for debugging
        System.out.println("Response Status Code: " + statusCode);
        System.out.println("Response Body: " + responseBody);

        // Return the JSON response
        return jsonResponse;
    }

    // Helper methods
    private void addActor(String name, String actorId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("actorId", actorId);
        sendRequest("addActor", "PUT", body);
    }
    private void addMovie(String name, String movieId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("movieId", movieId);
        sendRequest("addMovie", "PUT", body);
    }
    private void addRelationship(String actorId, String movieId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("actorId", actorId);
        body.put("movieId", movieId);
        sendRequest("addRelationship", "PUT", body);
    }
    private void addMovieWithRating(String name, String movieId, double rating) throws Exception {
        JSONObject addBody = new JSONObject();
        addBody.put("name", name);
        addBody.put("movieId", movieId);
        sendRequest("addMovie", "PUT", addBody);

        JSONObject rateBody = new JSONObject();
        rateBody.put("movieId", movieId);
        rateBody.put("rating", rating);
        sendRequest("addMovieRating", "PUT", rateBody);
    }

    public void testAddActorPass() throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", "John Doe");
        body.put("actorId", "nm1234567");

        JSONObject response = sendRequest("addActor", "PUT", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK: Actor added successfully", response.getString("body"));
    }

    public void testAddActorFail() throws Exception {
        // Test case 1: Missing required field
        JSONObject body = new JSONObject();
        body.put("name", "John Doe");

        JSONObject response = sendRequest("addActor", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));

        // Test case 2: Duplicate actor
        body = new JSONObject();
        body.put("name", "Bob");
        body.put("actorId", "nm1234567");
        sendRequest("addActor", "PUT", body);

        body = new JSONObject();
        body.put("name", "Bill");
        body.put("actorId", "nm1234567");
        response = sendRequest("addActor", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Actor already exists", response.getString("body"));
    }

    public void testAddMoviePass() throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", "Test Movie");
        body.put("movieId", "tt1234567");

        JSONObject response = sendRequest("addMovie", "PUT", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK: Movie added successfully", response.getString("body"));
    }

    public void testAddMovieFail() throws Exception {
        // Test case 1: Missing required field
        JSONObject body = new JSONObject();
        body.put("name", "Test Movie");

        JSONObject response = sendRequest("addMovie", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));

        // Test case 2: Duplicate movie
        body = new JSONObject();
        body.put("name", "Test Movie");
        body.put("movieId", "tt1234567");
        sendRequest("addMovie", "PUT", body);

        body = new JSONObject();
        body.put("name", "Cool Movie");
        body.put("movieId", "tt1234567");

        response = sendRequest("addMovie", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Movie already exists", response.getString("body"));
    }

    public void testGetActorPass() throws Exception {
        // Case 1: Actor acted in no movies
        // Add actor first
        addActor("John Doe", "nm1234567");

        // Get actor
        JSONObject getBody = new JSONObject();
        getBody.put("actorId", "nm1234567");
        JSONObject response = sendRequest("getActor", "GET", getBody);

        assertEquals(200, response.getInt("statusCode"));
//         assertEquals("OK", response.getString("body")); // OK

        // Verify the response content
        JSONObject responseBody = new JSONObject(response.getString("body"));
        assertEquals("nm1234567", responseBody.getString("actorId"));
        assertEquals("John Doe", responseBody.getString("name"));
        assertTrue(responseBody.has("movies"));

        // Verify the movies list is empty
        JSONArray moviesArray = responseBody.getJSONArray("movies");
        assertEquals(0, moviesArray.length());

        // Verify in Neo4j
        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (a:actor {id: $id}) RETURN a.name as name",
                    Values.parameters("id", "nm1234567")
            );
            assertTrue(result.hasNext());
            Record record = result.next();
            assertEquals("John Doe", record.get("name").asString());
        }

        // Case 2: Actor acted in movies
        // Add actor
        String actorId = "nm7654321";
        String actorName = "Jane Doe";
        addActor(actorName, actorId );

        // Add movies
        String movieId1 = "tt1234567";
        String movieName1 = "Movie One";
        addMovie(movieName1, movieId1);

        String movieId2 = "tt2345678";
        String movieName2 = "Movie Two";
        addMovie(movieName2, movieId2);

        // Add relationships
        addRelationship(actorId, movieId1);
        addRelationship(actorId, movieId2);

        // Get actor
        getBody = new JSONObject();
        getBody.put("actorId", actorId);
        response = sendRequest("getActor", "GET", getBody);

        assertEquals(200, response.getInt("statusCode"));

        // Verify the response content
        responseBody = new JSONObject(response.getString("body"));
        assertEquals(actorId, responseBody.getString("actorId"));
        assertEquals(actorName, responseBody.getString("name"));
        assertTrue(responseBody.has("movies"));

        // Verify the movies list is not empty and contains the correct movies
        moviesArray = responseBody.getJSONArray("movies");
        assertEquals(2, moviesArray.length());
        boolean containsMovie1 = false;
        boolean containsMovie2 = false;
        for (int i = 0; i < moviesArray.length(); i++) {
            String movieName = moviesArray.getString(i);
            if (movieName.equals(movieId1)) {
                containsMovie1 = true;
            } else if (movieName.equals(movieId2)) {
                containsMovie2 = true;
            }
        }
        assertTrue(containsMovie1);
        assertTrue(containsMovie2);

        // Verify in Neo4j
        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (a:actor {id: $id})-[:ACTED_IN]->(m:movie) RETURN m.name as name",
                    Values.parameters("id", actorId)
            );
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                Record record = result.next();
                String movieName = record.get("name").asString();
                assertTrue(movieName.equals(movieName1) || movieName.equals(movieName2));
            }
        }
    }

    public void testGetActorFail() throws Exception {
        // Test case 1: Non-existent actor
        JSONObject body = new JSONObject();
        body.put("actorId", "randomId");

        JSONObject response = sendRequest("getActor", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor does not exist", response.getString("body"));

        // Test case 2: Missing required field
        body = new JSONObject();
        body.put("wow", "wow");
        response = sendRequest("getActor", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));
    }

    public void testGetMoviePass() throws Exception {
        // Case 1: Movie with no one acted in it
        // Add Movie
        addMovie("Test Movie", "tt1234567");

        // Get Movie
        JSONObject getBody = new JSONObject();
        getBody.put("movieId", "tt1234567");

        JSONObject response = sendRequest("getMovie", "GET", getBody);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("{\"actors\":[],\"name\":\"Test Movie\",\"movieId\":\"tt1234567\"}", response.getString("body")); // OK

        // Verify the response content
        JSONObject responseBody = new JSONObject(response.getString("body"));
        assertEquals("tt1234567", responseBody.getString("movieId"));
        assertEquals("Test Movie", responseBody.getString("name"));
        assertTrue(responseBody.has("actors"));

        // Verify the actors list is empty
        JSONArray actorsArray = responseBody.getJSONArray("actors");
        assertEquals(0, actorsArray.length());

        // Verify in Neo4j
        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (m:movie {id: $id}) RETURN m.name as name",
                    Values.parameters("id", "tt1234567")
            );
            assertTrue(result.hasNext());
            Record record = result.next();
            assertEquals("Test Movie", record.get("name").asString());
        }

        // Case 2: Movie with actors
        String movieId = "tt2345678";
        String movieName = "Another Test Movie";
        addMovie(movieName, movieId);

        // Add actors
        String actorId1 = "nm1111111";
        String actorName1 = "Actor One";
        addActor(actorName1, actorId1);

        String actorId2 = "nm2222222";
        String actorName2 = "Actor Two";
        addActor(actorName2, actorId2);

        // Add relationships
        addRelationship(actorId1, movieId);
        addRelationship(actorId2, movieId);

        // Get Movie
        getBody = new JSONObject();
        getBody.put("movieId", movieId);

        response = sendRequest("getMovie", "GET", getBody);
        assertEquals(200, response.getInt("statusCode"));

        // Verify the response content
        responseBody = new JSONObject(response.getString("body"));
        assertEquals(movieId, responseBody.getString("movieId"));
        assertEquals(movieName, responseBody.getString("name"));
        assertTrue(responseBody.has("actors"));

        // Verify the actors list is not empty and contains the correct actors
        actorsArray = responseBody.getJSONArray("actors");
        assertEquals(2, actorsArray.length());

        boolean containsActor1 = false;
        boolean containsActor2 = false;

        for (int i = 0; i < actorsArray.length(); i++) {
            String actorName = actorsArray.getString(i);
            if (actorName.equals(actorId1)) {
                containsActor1 = true;
            } else if (actorName.equals(actorId2)) {
                containsActor2 = true;
            }
        }

        assertTrue(containsActor1);
        assertTrue(containsActor2);

        // Verify in Neo4j
        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (m:movie {id: $id})<-[:ACTED_IN]-(a:actor) RETURN a.name as name",
                    Values.parameters("id", movieId)
            );
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                Record record = result.next();
                String actorName = record.get("name").asString();
                assertTrue(actorName.equals(actorName1) || actorName.equals(actorName2));
            }
        }
    }

    public void testGetMovieFail() throws Exception {
        // Test case 1: Non-existent movie
        JSONObject body = new JSONObject();
        body.put("movieId", "tt9999999");
        JSONObject response = sendRequest("getMovie", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Movie does not exist", response.getString("body"));

        // Test case 2: Missing required field
        body = new JSONObject();
        response = sendRequest("getMovie", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));
    }

    public void testAddMovieRatingPass() throws Exception {
        // Add movie
        addMovie("Best Movie", "tt1234567");

        // Case 1: Rate the movie
        JSONObject rateBody = new JSONObject();
        rateBody.put("movieId", "tt1234567");
        rateBody.put("rating", 8.5);

        JSONObject response = sendRequest("addMovieRating", "PUT", rateBody);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK: Movie rating added/updated successfully", response.getString("body"));

        // delay between writing and reading the rating
        Thread.sleep(700);

        // Verify in Neo4j
        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (m:movie {id: $id}) RETURN m.rating as rating",
                    Values.parameters("id", "tt1234567")
            );
            assertTrue(result.hasNext());
            Record record = result.next();
            assertNotNull("Rating should not be null", record.get("rating"));
            assertEquals(8.5, record.get("rating").asFloat(), 0.01);
        }

        // Case 2: Rate the movie again
        rateBody = new JSONObject();
        rateBody.put("movieId", "tt1234567");
        rateBody.put("rating", 9.0);

        response = sendRequest("addMovieRating", "PUT", rateBody);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK: Movie rating added/updated successfully", response.getString("body"));

        // delay between writing and reading the rating
        Thread.sleep(700);

        // Verify updated rating in Neo4j
        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (m:movie {id: $id}) RETURN m.rating as rating",
                    Values.parameters("id", "tt1234567")
            );
            assertTrue(result.hasNext());
            Record record = result.next();
            assertNotNull("Rating should not be null", record.get("rating"));
            assertEquals(9.0, record.get("rating").asFloat(), 0.01);
        }
    }


    public void testAddMovieRatingFail() throws Exception {
        addMovie("w", "tt1234567");

        // Test case 1: Invalid rating (outside range)
        JSONObject body = new JSONObject();
        body.put("movieId", "tt1234567");
        body.put("rating", 11.0);

        JSONObject response = sendRequest("addMovieRating", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Rating must be between 0.0 and 10.0", response.getString("body"));

        // Test case 2: Non-existent movie
        body.put("movieId", "tt9999999");
        body.put("rating", 8.0);
        response = sendRequest("addMovieRating", "PUT", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Movie does not exist", response.getString("body"));

        // Test case 3: Missing required field
        body = new JSONObject();
        body.put("movieId", "tt1234567");
        response = sendRequest("addMovieRating", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));
    }

    public void testGetMoviesByRatingPass() throws Exception {
        // Add movies with different ratings
        addMovieWithRating("Movie1", "tt1111111", 7.0);
        addMovieWithRating("Movie2", "tt2222222", 8.0);
        addMovieWithRating("Movie3", "tt3333333", 9.0);

        // Get Movies By Rating
        JSONObject queryParams = new JSONObject();
        queryParams.put("minRating", 7.5);
        queryParams.put("maxRating", 8.5);

        JSONObject response = sendRequest("getMoviesByRating", "GET", queryParams);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("{\"movies\":[{\"name\":\"Movie2\",\"rating\":8,\"movieId\":\"tt2222222\"}]}", response.getString("body"));

        // Verify the response content
        JSONObject responseBody = new JSONObject(response.getString("body"));
        JSONArray movies = responseBody.getJSONArray("movies");
        assertEquals(1, movies.length());
        assertEquals("Movie2", movies.getJSONObject(0).getString("name"));
    }



    public void testGetMoviesByRatingFail() throws Exception {
        // Test case 1: Invalid range (minRating > maxRating)
        JSONObject body = new JSONObject();
        body.put("minRating", 9.0);
        body.put("maxRating", 7.0);

        JSONObject response = sendRequest("getMoviesByRating", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: minRating must be less than or equal to maxRating", response.getString("body"));

        // Test case 2: Rating outside valid range
        body = new JSONObject();
        body.put("minRating", -1.0);
        body.put("maxRating", 11.0);
        response = sendRequest("getMoviesByRating", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: minRating must be between 0.0 and 10.0", response.getString("body"));

        // Test case 3: Missing required field
        body = new JSONObject();
        body.put("minRating", 7.0);
        response = sendRequest("getMoviesByRating", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));
    }

    public void testAddRelationshipPass() throws Exception {
        addActor("John Doe", "nm1234567");
        addMovie("Test Movie", "tt1234567");

        JSONObject body = new JSONObject();
        body.put("actorId", "nm1234567");
        body.put("movieId", "tt1234567");

        JSONObject response = sendRequest("addRelationship", "PUT", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK: Relationship added successfully", response.getString("body"));

        Thread.sleep(700);

        // Verify relationship in Neo4j
        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (a:actor {id: $actorId})-[r:ACTED_IN]->(m:movie {id: $movieId}) RETURN COUNT(r) as count",
                    Values.parameters("actorId", "nm1234567", "movieId", "tt1234567")
            );
            assertEquals(1, result.single().get("count").asInt());
        }
    }

    public void testAddRelationshipFail() throws Exception {
        // Test case 1: Non-existent actor
        addMovie("Wow", "tt1");
        JSONObject body = new JSONObject();
        body.put("actorId", "nm9999999");
        body.put("movieId", "tt1");

        JSONObject response = sendRequest("addRelationship", "PUT", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor and/or movie does not exist", response.getString("body"));

        // Test case 2: Duplicate relationship
        addActor("John Doe", "nm1234567");
        addMovie("Test Movie", "tt1234567");
        body = new JSONObject();
        body.put("actorId", "nm1234567");
        body.put("movieId", "tt1234567");
        sendRequest("addRelationship", "PUT", body); // 1st add
        response = sendRequest("addRelationship", "PUT", body); // 2nd add
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Relationship already exists", response.getString("body"));

        Thread.sleep(700);

        try (Session session = Utils.driver.session()) {
            StatementResult result = session.run(
                    "MATCH (a:actor {id: $actorId})-[r:ACTED_IN]->(m:movie {id: $movieId}) RETURN COUNT(r) as count",
                    Values.parameters("actorId", "nm1234567", "movieId", "tt1234567")
            );
            assertEquals(1, result.single().get("count").asInt());
        }

        // Test case 3: Missing required field
        body = new JSONObject();
        body.put("actorId", "nm1234567");
        response = sendRequest("addRelationship", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));

        // Test case 4: Non-existent movie
        body = new JSONObject();
        body.put("actorId", "nm1234567");
        body.put("movieId", "tt9999999");
        response = sendRequest("addRelationship", "PUT", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor and/or movie does not exist", response.getString("body"));
    }

    public void testHasRelationshipPass() throws Exception {
        addActor("John Doe", "nm1234567");
        addMovie("Test Movie", "tt1234567");
        addRelationship("nm1234567", "tt1234567");

        // Test case 1: Actor and movie exist with relationship
        JSONObject body = new JSONObject();
        body.put("actorId", "nm1234567");
        body.put("movieId", "tt1234567");

        JSONObject response = sendRequest("hasRelationship", "GET", body);
        assertEquals(200, response.getInt("statusCode"));

        JSONObject responseBody = new JSONObject(response.getString("body"));
        assertEquals("nm1234567", responseBody.getString("actorId"));
        assertEquals("tt1234567", responseBody.getString("movieId"));
        assertTrue(responseBody.getBoolean("hasRelationship"));

        // Test case 2: Actor and movie exist but no relationship
        addActor("A", "acted123");
        addMovie("B", "movie123");
        body.put("actorId", "acted123");
        body.put("movieId", "movie123");
        response = sendRequest("hasRelationship", "GET", body);
        assertEquals(200, response.getInt("statusCode"));
        responseBody = new JSONObject(response.getString("body"));
        assertFalse(responseBody.getBoolean("hasRelationship"));
    }

    public void testHasRelationshipFail() throws Exception {
        addActor("wow", "act123");
        addMovie("movie", "movie123");

        // Test case 1: Non-existent actor & movie
        JSONObject body = new JSONObject();
        body.put("actorId", "nm9999999");
        body.put("movieId", "tt1234567");

        JSONObject response = sendRequest("hasRelationship", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor or Movie does not exist", response.getString("body"));

        // Test case 2: Non-existent actor
        body = new JSONObject();
        body.put("actorId", "nm9999999");
        body.put("movieId", "movie123");

        response = sendRequest("hasRelationship", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor or Movie does not exist", response.getString("body"));

        // Test case 3: Non-existent movie
        body = new JSONObject();
        body.put("actorId", "act123");
        body.put("movieId", "wowww");

        response = sendRequest("hasRelationship", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor or Movie does not exist", response.getString("body"));

        // Test case 4: Missing required field
        body = new JSONObject();
        body.put("actorId", "nm1234567");
        response = sendRequest("hasRelationship", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));
    }

    public void testComputeBaconNumberPass() throws Exception {
        // Setup
        addActor("Kevin Bacon", "nm0000102");
        addMovie("Movie A", "tt0000001");
        addActor("Actor A", "nm0000001");
        addMovie("Movie B", "tt0000002");
        addActor("Actor B", "nm0000002");

        // Create relationships
        JSONObject relBody = new JSONObject();
        relBody.put("actorId", "nm0000102");
        relBody.put("movieId", "tt0000001");
        sendRequest("addRelationship", "PUT", relBody);

        relBody.put("actorId", "nm0000001");
        relBody.put("movieId", "tt0000001");
        sendRequest("addRelationship", "PUT", relBody);

        relBody.put("actorId", "nm0000001");
        relBody.put("movieId", "tt0000002");
        sendRequest("addRelationship", "PUT", relBody);

        relBody.put("actorId", "nm0000002");
        relBody.put("movieId", "tt0000002");
        sendRequest("addRelationship", "PUT", relBody);

        // Test case 1: Normal Bacon number
        JSONObject body = new JSONObject();
        body.put("actorId", "nm0000002");

        JSONObject response = sendRequest("computeBaconNumber", "GET", body);
        assertEquals(200, response.getInt("statusCode"));
        JSONObject responseBody = new JSONObject(response.getString("body"));
        assertEquals(2, responseBody.getInt("baconNumber"));

        // Test case 2: Kevin Bacon himself
        body.put("actorId", "nm0000102");
        response = sendRequest("computeBaconNumber", "GET", body);
        assertEquals(200, response.getInt("statusCode"));
        responseBody = new JSONObject(response.getString("body"));
        assertEquals(0, responseBody.getInt("baconNumber"));
    }

    public void testComputeBaconNumberFail() throws Exception {
        // Test case 1: Non-existent actor
        JSONObject body = new JSONObject();
        body.put("actorId", "nm9999999");

        JSONObject response = sendRequest("computeBaconNumber", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor does not exist or has no path to Kevin Bacon", response.getString("body"));

        // Test case 2: Missing required field
        body = new JSONObject();
        response = sendRequest("computeBaconNumber", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));

        // Test case 3: Actor exists but has no path to Kevin Bacon
        addActor("Isolated Actor", "nm0000099");
        body.put("actorId", "nm0000099");
        response = sendRequest("computeBaconNumber", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor does not exist or has no path to Kevin Bacon", response.getString("body"));
    }

    public void testComputeBaconPathPass() throws Exception {
        // Setup
        addActor("Kevin Bacon", "nm0000102");
        addMovie("Movie A", "tt0000001");
        addActor("Actor A", "nm0000001");
        addMovie("Movie B", "tt0000002");
        addActor("Actor B", "nm0000002");

        // Create relationships
        JSONObject relBody = new JSONObject();
        relBody.put("actorId", "nm0000102");
        relBody.put("movieId", "tt0000001");
        sendRequest("addRelationship", "PUT", relBody);

        relBody.put("actorId", "nm0000001");
        relBody.put("movieId", "tt0000001");
        sendRequest("addRelationship", "PUT", relBody);

        relBody.put("actorId", "nm0000001");
        relBody.put("movieId", "tt0000002");
        sendRequest("addRelationship", "PUT", relBody);

        relBody.put("actorId", "nm0000002");
        relBody.put("movieId", "tt0000002");
        sendRequest("addRelationship", "PUT", relBody);

        JSONObject body = new JSONObject();
        body.put("actorId", "nm0000002");

        // Test case 1: Normal Bacon number
        JSONObject response = sendRequest("computeBaconPath", "GET", body);
        assertEquals(200, response.getInt("statusCode"));

        JSONObject responseBody = new JSONObject(response.getString("body"));
        assertTrue(responseBody.has("baconPath"));

        JSONArray baconPath = responseBody.getJSONArray("baconPath");
        assertEquals(5, baconPath.length());
        assertEquals("nm0000002", baconPath.getString(0));
        assertEquals("tt0000002", baconPath.getString(1));
        assertEquals("nm0000001", baconPath.getString(2));
        assertEquals("tt0000001", baconPath.getString(3));
        assertEquals("nm0000102", baconPath.getString(4));

        // Test case 2: Kevin Bacon himself
        body.put("actorId", "nm0000102");
        response = sendRequest("computeBaconPath", "GET", body);
        assertEquals(200, response.getInt("statusCode"));
        responseBody = new JSONObject(response.getString("body"));
        assertTrue(responseBody.has("baconPath"));
        assertEquals(1, responseBody.getJSONArray("baconPath").length());
        assertEquals("nm0000102", responseBody.getJSONArray("baconPath").getString(0));
    }

    public void testComputeBaconPathFail() throws Exception {
        // Test case 1: Non-existent actor
        JSONObject body = new JSONObject();
        body.put("actorId", "nm9999999");

        JSONObject response = sendRequest("computeBaconPath", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor does not exist or has no path to Kevin Bacon", response.getString("body"));

        // Test case 2: Missing required field
        body = new JSONObject();
        response = sendRequest("computeBaconPath", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));

        // Test case 3: Actor exists but has no path to Kevin Bacon
        addActor("Isolated Actor", "nm0000099");
        body.put("actorId", "nm0000099");
        response = sendRequest("computeBaconPath", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor does not exist or has no path to Kevin Bacon", response.getString("body"));
    }

    public void testGetActorNetworkPass() throws Exception {
        // Setup: Add actors and their relationships
        addActor("Kevin Bacon", "nm0000102");
        addActor("Actor A", "nm0000001");
        addActor("Actor B", "nm0000002");
        addMovie("Movie X", "tt0000001");
        addRelationship("nm0000102", "tt0000001");
        addRelationship("nm0000001", "tt0000001");
        addRelationship("nm0000002", "tt0000001");

        // Test case 1: Actor with collaborations in one movie
        JSONObject body = new JSONObject();
        body.put("actorId", "nm0000102");
        JSONObject response = sendRequest("getActorNetwork", "GET", body);
        assertEquals(200, response.getInt("statusCode"));

        JSONObject responseBody = new JSONObject(response.getString("body"));
        assertTrue(responseBody.has("actors"));
        JSONArray actors = responseBody.getJSONArray("actors");
        assertEquals(2, actors.length());
        assertTrue(actors.toString().contains("nm0000001"));
        assertTrue(actors.toString().contains("nm0000002"));

        // Test case 2: Actor with collaborations across two movies
        addMovie("Movie Y", "tt0000002");
        addActor("Actor C", "nm0000003");
        addRelationship("nm0000102", "tt0000002");
        addRelationship("nm0000003", "tt0000002");

        body = new JSONObject();
        body.put("actorId", "nm0000102");
        response = sendRequest("getActorNetwork", "GET", body);
        assertEquals(200, response.getInt("statusCode"));

        responseBody = new JSONObject(response.getString("body"));
        assertTrue(responseBody.has("actors"));
        actors = responseBody.getJSONArray("actors");
        assertEquals(3, actors.length());
        assertTrue(actors.toString().contains("nm0000001"));
        assertTrue(actors.toString().contains("nm0000002"));
        assertTrue(actors.toString().contains("nm0000003"));

        // Test case 3: Isolated actor (no collaborations)
        addActor("Isolated Actor", "nm0000099");
        body.put("actorId", "nm0000099");
        response = sendRequest("getActorNetwork", "GET", body);
        assertEquals(200, response.getInt("statusCode"));
        responseBody = new JSONObject(response.getString("body"));
        assertTrue(responseBody.has("actors"));
        actors = responseBody.getJSONArray("actors");
        assertEquals(0, actors.length());
    }

    public void testGetActorNetworkFail() throws Exception {
        // Test case 1: Non-existent actor
        JSONObject body = new JSONObject();
        body.put("actorId", "nm9999999");
        JSONObject response = sendRequest("getActorNetwork", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found: Actor does not exist", response.getString("body"));

        // Test case 2: Missing required field
        body = new JSONObject();
        response = sendRequest("getActorNetwork", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Malformed JSON or missing required fields", response.getString("body"));
    }

    public void testGetConnectStatsPass() throws Exception {
        // Test case 1: Empty database
        JSONObject response = sendRequest("getConnectStats", "GET", new JSONObject()
                .put("limit", 5)
                .put("most", true));
        assertEquals(200, response.getInt("statusCode"));
        JSONObject responseBody = new JSONObject(response.getString("body"));
        assertTrue(responseBody.has("actors"));
        JSONArray actors = responseBody.getJSONArray("actors");
        assertEquals(0, actors.length());

        // Setup: Add actors and their relationships
        addActor("Actor A", "nm0000001");
        addActor("Actor B", "nm0000002");
        addActor("Actor C", "nm0000003");
        addActor("Actor D", "nm0000004");
        addActor("Actor E", "nm0000005");

        addMovie("Movie 1", "tt0000001");
        addMovie("Movie 2", "tt0000002");
        addMovie("Movie 3", "tt0000003");
        addMovie("Movie 4", "tt0000004");
        addMovie("Movie 5", "tt0000005");

        // Actor A: 4 connections
        addRelationship("nm0000001", "tt0000001");
        addRelationship("nm0000001", "tt0000002");
        addRelationship("nm0000001", "tt0000003");
        addRelationship("nm0000001", "tt0000004");

        // Actor B: 3 connections
        addRelationship("nm0000002", "tt0000001");
        addRelationship("nm0000002", "tt0000002");
        addRelationship("nm0000002", "tt0000003");

        // Actor C: 2 connections
        addRelationship("nm0000003", "tt0000001");
        addRelationship("nm0000003", "tt0000002");

        // Actor D: 1 connection
        addRelationship("nm0000004", "tt0000001");

        // Actor E: 0 connections

        // Test case 2: Get actors with most connections (limit 3)
        response = sendRequest("getConnectStats", "GET", new JSONObject()
                .put("limit", 3)
                .put("most", true));
        assertEquals(200, response.getInt("statusCode"));

        responseBody = new JSONObject(response.getString("body"));
        assertTrue(responseBody.has("actors"));
        actors = responseBody.getJSONArray("actors");
        assertEquals(3, actors.length());
        assertEquals("nm0000001", actors.getJSONObject(0).getString("actorId"));
        assertEquals(4, actors.getJSONObject(0).getInt("connections"));
        assertEquals("nm0000002", actors.getJSONObject(1).getString("actorId"));
        assertEquals(3, actors.getJSONObject(1).getInt("connections"));
        assertEquals("nm0000003", actors.getJSONObject(2).getString("actorId"));
        assertEquals(2, actors.getJSONObject(2).getInt("connections"));

        // Test case 3: Get actors with least connections (limit 3)
        response = sendRequest("getConnectStats", "GET", new JSONObject()
                .put("limit", 3)
                .put("most", false));
        assertEquals(200, response.getInt("statusCode"));

        responseBody = new JSONObject(response.getString("body"));
        actors = responseBody.getJSONArray("actors");
        assertEquals(3, actors.length());
        assertEquals("nm0000005", actors.getJSONObject(0).getString("actorId"));
        assertEquals(0, actors.getJSONObject(0).getInt("connections"));
        assertEquals("nm0000004", actors.getJSONObject(1).getString("actorId"));
        assertEquals(1, actors.getJSONObject(1).getInt("connections"));
        assertEquals("nm0000003", actors.getJSONObject(2).getString("actorId"));
        assertEquals(2, actors.getJSONObject(2).getInt("connections"));

        // Test case 4: Limit greater than number of actors
        response = sendRequest("getConnectStats", "GET", new JSONObject()
                .put("limit", 10)
                .put("most", true));
        assertEquals(200, response.getInt("statusCode"));
        responseBody = new JSONObject(response.getString("body"));
        actors = responseBody.getJSONArray("actors");
        assertEquals(5, actors.length());  // Should return all actors

        // Test case 5: Limit of 0
        response = sendRequest("getConnectStats", "GET", new JSONObject()
                .put("limit", 0)
                .put("most", true));
        assertEquals(200, response.getInt("statusCode"));
        responseBody = new JSONObject(response.getString("body"));
        actors = responseBody.getJSONArray("actors");
        assertEquals(0, actors.length());
    }

    public void testGetConnectStatsFail() throws Exception {
        // Setup: Add actors and their relationships
        addActor("Kevin Bacon", "nm0000102");
        addActor("Actor A", "nm0000001");
        addActor("Actor B", "nm0000002");
        addActor("Actor C", "nm0000003");
        addMovie("Movie X", "tt0000001");
        addMovie("Movie Y", "tt0000002");
        addRelationship("nm0000102", "tt0000001");
        addRelationship("nm0000001", "tt0000001");
        addRelationship("nm0000002", "tt0000001");
        addRelationship("nm0000102", "tt0000002");
        addRelationship("nm0000001", "tt0000002");

        // Test case 1: Negative limit
        JSONObject response = sendRequest("getConnectStats", "GET", new JSONObject()
                .put("limit", -1)
                .put("most", true));
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request: Limit cannot be negative", response.getString("body"));
    }

}

