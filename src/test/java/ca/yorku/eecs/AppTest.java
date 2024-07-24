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
    private static final String BASE_URL = "http://localhost:8080";

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

    public void testAddMovieRatingPass() throws Exception {
        JSONObject json1 = new JSONObject();
        json1.put("name", "Great Movie");
        json1.put("movieId", "tt1234567");
        sendRequest("/api/v1/addMovie", "PUT", json1);

        JSONObject json2 = new JSONObject();
        json2.put("movieId", "tt1234567");
        json2.put("rating", 5);
        HttpURLConnection connection = sendRequest("/api/v1/addMovieRating", "PUT", json2);

        assertEquals(200, connection.getResponseCode());
        connection.disconnect();
    }

    public void testAddMovieRatingFail() throws Exception {
        JSONObject json = new JSONObject();
        // Add rating but movie does not exist
        json.put("movieId", "non-existing");
        json.put("rating", 5);

        HttpURLConnection connection = sendRequest("/api/v1/addMovieRating", "PUT", json);

        assertEquals(404, connection.getResponseCode());
        connection.disconnect();
    }

    public void testAddActorPass() throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", "John Doe");
        json.put("actorId", "nm1234567");

        HttpURLConnection connection = sendRequest("/api/v1/addActor", "PUT", json);

        assertEquals(200, connection.getResponseCode());
        connection.disconnect();
    }

    public void testAddActorFail() throws Exception {
        JSONObject json = new JSONObject();
        // Missing required field 'name'
        json.put("actorId", "nm1234567");

        HttpURLConnection connection = sendRequest("/api/v1/addActor", "PUT", json);

        assertEquals(400, connection.getResponseCode());
        connection.disconnect();
    }

    private HttpURLConnection sendRequest(String path, String method, JSONObject body) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        return connection;
    }



}

