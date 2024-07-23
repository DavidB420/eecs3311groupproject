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

public class AddRelationshipHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

    }
}