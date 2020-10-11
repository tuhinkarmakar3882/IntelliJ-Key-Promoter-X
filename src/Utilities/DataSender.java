package Utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.halirutan.keypromoterx.KeyPromoterAction;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Properties;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class DataSender {
  private final String url;
  private final String username;
  private final String password;
  private final String payload;

  public DataSender(KeyPromoterAction action) throws JsonProcessingException {
    this.url = readFromConfigFile("CLOUD_FUNCTION");
    this.username = readFromConfigFile("USERNAME");
    this.password = readFromConfigFile("PASSWORD");
    this.payload = generatePayload(action);
  }

  private String readFromConfigFile(String key) {
    Properties prop = new Properties();

    String filePath = System.getProperty("user.home") + "/.t-key-promoter-x/config.properties";

    try (FileInputStream ip = new FileInputStream(filePath)) {
      prop.load(ip);
      return prop.getProperty(key);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void sendToServer() {
    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(ofString(payload))
            .build();

    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(System.out::println)
            .exceptionally(e -> {
              //  Todo Think about what to do when it fails
              System.out.println(e.getMessage());
              return null;
            });
  }

  private String generatePayload(KeyPromoterAction action) throws JsonProcessingException {
    HashMap<String, String> payload = new HashMap<>();

    payload.put("username", username);
    payload.put("password", password);
    payload.put("actionMissed", action.getDescription());
    payload.put("actionShortcut", action.getShortcut());
    payload.put("timestamp", String.valueOf(System.currentTimeMillis()));

    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(payload);
  }
}
