/*
 * Copyright (c) 2017 Patrick Scheibe, Dmitry Kashin, Athiele.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.halirutan.keypromoterx;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.keymap.impl.ui.EditKeymapsDialog;
import de.halirutan.keypromoterx.statistic.KeyPromoterStatistics;
import org.jetbrains.annotations.NotNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.HashMap;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

/**
 * A custom notification class that allows for creating 1. tips if a short cut was missed and 2. a balloon asking if
 * the user wants to create a shortcut for an action that doesn't have one.
 *
 * @author Patrick Scheibe.
 */
class KeyPromoterNotification {

  private static final NotificationGroup GROUP = new NotificationGroup(
      KeyPromoterBundle.message("kp.notification.group"),
      NotificationDisplayType.BALLOON,
      false,
      KeyPromoterBundle.message("kp.tool.window.name"),
      KeyPromoterIcons.KP_ICON
  );
  private static final KeyPromoterSettings settings = ServiceManager.getService(KeyPromoterSettings.class);

  static void showTip(KeyPromoterAction action, int count) {
    String message = KeyPromoterBundle.message("kp.notification.tip", action.getDescription(), count);
    final Notification notification = GROUP.createNotification(KeyPromoterBundle.message(
        "kp.notification.group"),
        message,
        NotificationType.INFORMATION, null)
                                           .setIcon(KeyPromoterIcons.KP_ICON)
                                           .addAction(new EditKeymapAction(action, action.getShortcut()))
                                           .addAction(new SuppressTipAction(action));
    notification.notify(null);

    try {
      String url = "http://localhost:5001/key-promoter-x---report/us-central1/report/add";
      String payload = generatePayload(action);
      sendToServer(url, payload);
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }

  private static void sendToServer(String url, String payload) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(ofString(payload))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.body());
  }

  private static String generatePayload(KeyPromoterAction action) throws JsonProcessingException {
    HashMap<String, String> payload = new HashMap<>();

    payload.put("user", "Alice Bob");
    payload.put("actionMissed", action.getDescription());
    payload.put("actionShortcut", action.getShortcut());
    payload.put("eventTime", String.valueOf(new Timestamp(System.currentTimeMillis())));

    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(payload);
  }

  static void askToCreateShortcut(KeyPromoterAction action) {
    Notification notification = GROUP.createNotification(
        KeyPromoterBundle.message("kp.notification.group"),
        KeyPromoterBundle.message("kp.notification.ask.new.shortcut", action.getDescription()),
        NotificationType.INFORMATION,
        null
    )
                                     .setIcon(KeyPromoterIcons.KP_ICON)
                                     .addAction(new EditKeymapAction(action))
                                     .addAction(new SuppressTipAction(action));
    notification.notify(null);
  }


  /**
   * Provides click-able links to IDEA actions. On click, the keymap editor is opened showing the exact line where
   * the shortcut of an action can be edited/created.
   */
  private static class EditKeymapAction extends NotificationAction {
    private final KeyPromoterAction myAction;

    EditKeymapAction(KeyPromoterAction action) {
      super(action.getDescription());
      this.myAction = action;
    }

    EditKeymapAction(KeyPromoterAction action, String buttonText) {
      super(buttonText);
      this.myAction = action;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
      EditKeymapsDialog dialog = new EditKeymapsDialog(null, myAction.getIdeaActionID());
      ApplicationManager.getApplication().invokeLater(dialog::show);
    }
  }

  private static class SuppressTipAction extends NotificationAction {
    private final KeyPromoterStatistics statistics = ServiceManager.getService(KeyPromoterStatistics.class);
    private final KeyPromoterAction myAction;

    SuppressTipAction(KeyPromoterAction action) {
      super("(Don't show again)");
      myAction = action;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
      statistics.suppressItem(myAction);
      notification.expire();
    }
  }


}
