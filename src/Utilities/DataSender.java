package Utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.halirutan.keypromoterx.KeyPromoterAction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.HashMap;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class DataSender {
  private final String url;
  private final String payload;

  public DataSender(String url, KeyPromoterAction action) throws JsonProcessingException {
    this.url = url;
    this.payload = generatePayload(action);
  }

  public void sendToServer() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(ofString(payload))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.body());
  }

  private String generatePayload(KeyPromoterAction action) throws JsonProcessingException {
    HashMap<String, String> payload = new HashMap<>();

    payload.put("user", "Alice Bob");
    payload.put("actionMissed", action.getDescription());
    payload.put("actionShortcut", action.getShortcut());
    payload.put("eventTime", String.valueOf(new Timestamp(System.currentTimeMillis())));

    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(payload);
  }
}
