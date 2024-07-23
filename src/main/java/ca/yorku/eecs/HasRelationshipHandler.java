package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;

import java.io.IOException;
import java.io.OutputStream;

public class HasRelationshipHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

    }
}