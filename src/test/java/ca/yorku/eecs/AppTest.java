package ca.yorku.eecs;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;

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
            System.out.println("Database cleared.");
        } catch (Exception e) {
            System.err.println("Error during setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private JSONObject sendRequest(String endpoint, String method, JSONObject params) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + endpoint);

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);

        if (params != null) {
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = params.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
        }

        int status = con.getResponseCode();
        String statusMessage = con.getResponseMessage();

        con.disconnect();

        JSONObject response = new JSONObject();
        response.put("statusCode", status);
        response.put("statusMessage", statusMessage);
        return response;
    }
    public void testAddActorPass() throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", "John Doe");
        body.put("actorId", "nm1234567");

        JSONObject response = sendRequest("addActor", "PUT", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK", response.getString("statusMessage"));
    }

    public void testAddActorFail() throws Exception {
        // Test case 1: Missing required field
        JSONObject body = new JSONObject();
        body.put("name", "John Doe");

        JSONObject response = sendRequest("addActor", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));

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
        assertEquals("Bad Request", response.getString("statusMessage"));
    }

    public void testAddMoviePass() throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", "Test Movie");
        body.put("movieId", "tt1234567");

        JSONObject response = sendRequest("addMovie", "PUT", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK", response.getString("statusMessage"));
    }

    public void testAddMovieFail() throws Exception {
        // Test case 1: Missing required field
        JSONObject body = new JSONObject();
        body.put("name", "Test Movie");

        JSONObject response = sendRequest("addMovie", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));

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
        assertEquals("Bad Request", response.getString("statusMessage"));
    }

    public void testGetActorPass() throws Exception {
        // Add actor first
        JSONObject body = new JSONObject();
        body.put("name", "John Doe");
        body.put("actorId", "nm1234567");
        sendRequest("addActor", "PUT", body);

        body = new JSONObject();
        body.put("actorId", "nm1234567");
        JSONObject response = sendRequest("getActor", "GET", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK", response.getString("statusMessage"));
    }

    public void testGetActorFail() throws Exception {
        // Test case 1: Non-existent actor
        JSONObject body = new JSONObject();
        body.put("actorId", "randomId");

        JSONObject response = sendRequest("getActor", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found", response.getString("statusMessage"));

        // Test case 2: Missing required field
        body = new JSONObject();
        response = sendRequest("getActor", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));

        body = new JSONObject();
        body.put("id", "wow");
        response = sendRequest("getActor", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));
    }

    public void testGetMoviePass() throws Exception {
        // Add Movie
        JSONObject body = new JSONObject();
        body.put("name", "Test Movie");
        body.put("movieId", "tt1234567");
        sendRequest("addMovie", "PUT", body);

        // Get Movie
        body = new JSONObject();
        body.put("movieId", "tt1234567");

        JSONObject response = sendRequest("getMovie", "GET", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK", response.getString("statusMessage"));
    }

    public void testGetMovieFail() throws Exception {
        // Test case 1: Non-existent movie
        JSONObject body = new JSONObject();
        body.put("movieId", "tt9999999");

        JSONObject response = sendRequest("getMovie", "GET", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found", response.getString("statusMessage"));

        // Test case 2: Missing required field
        body = new JSONObject();
        response = sendRequest("getMovie", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));
    }

    public void testAddMovieRatingPass() throws Exception {
        // Add movie
        JSONObject body = new JSONObject();
        body.put("name", "Best Movie");
        body.put("movieId", "tt1234567");
        sendRequest("addMovie", "PUT", body);

        // Case 1: Rate the movie
        body = new JSONObject();
        body.put("movieId", "tt1234567");
        body.put("rating", 8.5);

        JSONObject response = sendRequest("addMovieRating", "PUT", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK", response.getString("statusMessage"));

        // Case 2: Rate the movie again
        body = new JSONObject();
        body.put("movieId", "tt1234567");
        body.put("rating", 1.0);

        response = sendRequest("addMovieRating", "PUT", body);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK", response.getString("statusMessage"));
    }

    public void testAddMovieRatingFail() throws Exception {
        // Test case 1: Invalid rating (outside range)
        JSONObject body = new JSONObject();
        body.put("movieId", "tt1234567");
        body.put("rating", 11.0);

        JSONObject response = sendRequest("addMovieRating", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));

        // Test case 2: Non-existent movie
        body.put("movieId", "tt9999999");
        body.put("rating", 8.0);
        response = sendRequest("addMovieRating", "PUT", body);
        assertEquals(404, response.getInt("statusCode"));
        assertEquals("Not Found", response.getString("statusMessage"));

        // Test case 3: Missing required field
        body = new JSONObject();
        body.put("movieId", "tt1234567");
        response = sendRequest("addMovieRating", "PUT", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));
    }

    public void testGetMoviesByRatingPass() throws Exception {
        // Add movie
        JSONObject body = new JSONObject();
        body.put("name", "Best Movie");
        body.put("movieId", "tt1234567");
        sendRequest("addMovie", "PUT", body);

        // Rate the movie
        body = new JSONObject();
        body.put("movieId", "tt1234567");
        body.put("rating", 8.5);
        sendRequest("addMovieRating", "PUT", body);

        // Get Movies By Rating, minRating = maxRating
        JSONObject queryParams = new JSONObject();
        queryParams.put("minRating", 7.5);
        queryParams.put("maxRating", 7.5);

        JSONObject response = sendRequest("getMoviesByRating", "GET", queryParams);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK", response.getString("statusMessage"));

        // Get Movies By Rating
        queryParams = new JSONObject();
        queryParams.put("minRating", 7.5);
        queryParams.put("maxRating", 10.0);

        response = sendRequest("getMoviesByRating", "GET", queryParams);
        assertEquals(200, response.getInt("statusCode"));
        assertEquals("OK", response.getString("statusMessage"));
    }

    public void testGetMoviesByRatingFail() throws Exception {
        // Test case 1: Invalid range (minRating > maxRating)
        JSONObject body = new JSONObject();
        body.put("minRating", 9.0);
        body.put("maxRating", 7.0);

        JSONObject response = sendRequest("getMoviesByRating", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));

        // Test case 2: Rating outside valid range
        body = new JSONObject();
        body.put("minRating", -1.0);
        body.put("maxRating", 11.0);
        response = sendRequest("getMoviesByRating", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));

        // Test case 3: Missing required field
        body = new JSONObject();
        body.put("minRating", 7.0);
        response = sendRequest("getMoviesByRating", "GET", body);
        assertEquals(400, response.getInt("statusCode"));
        assertEquals("Bad Request", response.getString("statusMessage"));
    }
}

