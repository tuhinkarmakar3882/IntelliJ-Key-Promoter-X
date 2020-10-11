package Utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.halirutan.keypromoterx.KeyPromoterAction;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Properties;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class DataSender {
  private final String url;
  private final String username;
  private final String payload;

  public DataSender(KeyPromoterAction action) throws JsonProcessingException {
    this.url = readFromConfigFile("CLOUD_FUNCTION");
    this.username = readFromConfigFile("USERNAME");
    this.payload = generatePayload(action);
  }

  private String readFromConfigFile(String key) {
    Properties prop = new Properties();

    String filePath = System.getProperty("user.home") + "/.keyPromoter/config.properties";

    try (FileInputStream ip = new FileInputStream(filePath)) {
      prop.load(ip);
      return prop.getProperty(key);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void sendToServer() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(ofString(payload))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    // Todo For Debug Only. Remember to remove it following line
    System.out.println(response.body());
  }

  private String generatePayload(KeyPromoterAction action) throws JsonProcessingException {
    HashMap<String, String> payload = new HashMap<>();

    payload.put("username", username);
    payload.put("actionMissed", action.getDescription());
    payload.put("actionShortcut", action.getShortcut());
    payload.put("timestamp", String.valueOf(new Timestamp(System.currentTimeMillis())));

    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(payload);
  }
}
