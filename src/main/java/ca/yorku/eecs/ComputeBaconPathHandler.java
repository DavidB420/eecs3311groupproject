package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;

import java.io.IOException;
import java.io.OutputStream;

public class ComputeBaconPathHandler implements HttpHandler {
    private static final String KEVIN_BACON_ID = "nm0000102";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        
    }
}