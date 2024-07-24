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
        try {
            if (exchange.getRequestMethod().equals("GET")) {
                handleGet(exchange);
            } else {
                // Method not allowed for non-GET requests
                Utils.sendResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    public void handleGet(HttpExchange exchange) throws IOException, JSONException{
        String body = Utils.convert(exchange.getRequestBody());
        JSONObject jo;
        String movieId, actorId;

        try{
            jo = new JSONObject(body);
            actorId = jo.getString("actorId");
            movieId = jo.getString("movieId");
        }catch(JSONException e){
            Utils.sendResponse(exchange, 400, "Bad Request: Malformed JSON or missing required fields");
            return;
        }
        
        try (Session session = Utils.driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                // Check if actor and movie already exists
                StatementResult resultActor = tx.run("MATCH (a:actor {id: $id}) RETURN a",
                Values.parameters("id", actorId));
                StatementResult resultMovie = tx.run("MATCH (a:movie {id: $id}) RETURN a",
                Values.parameters("id", movieId));
                JSONObject json = new JSONObject();
                if (resultActor.hasNext() && resultMovie.hasNext()){

                    json.put("actorId",actorId);
                    json.put("movieId",movieId);
                    json.put("hasRelationship",checkRelationship(tx, movieId, actorId));

                    Utils.sendResponse(exchange, 200, json.toString());
                }
                else{
                    Utils.sendResponse(exchange, 404, "Not Found: Actor or Movie does not exist");
                }
            }catch (JSONException e){
                Utils.sendResponse(exchange, 500, "Bad Request: Malformed JSON or missing required fields");
            }
        }
    }

    private boolean checkRelationship(Transaction tx, String mId, String aId){
        try{
            StatementResult resultQuery = tx.run("MATCH (a:actor {id: $actorId})-[r:ACTED_IN]->(m:movie {id: $movieId}) RETURN COUNT(r) > 0 as hasRelationship",
            Values.parameters("movieId",mId,"actorId",aId));
    
            return resultQuery.single().get("hasRelationship").asBoolean();
        }catch (Exception e){
            return false;
        }
    }
}