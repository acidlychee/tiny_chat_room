package com.exercise.chat;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
public class ChatRoomController {

  @Autowired
  private SimpMessageSendingOperations messagingTemplate;

  private static Map<String, String> userAndRoom = new HashMap<>();

  private static Map<String, Set<String>> roomAndUsers = new HashMap<>();

  private static Map<String, String> userAndName = new HashMap<>();

  @MessageMapping("/room")
  @SendTo("/topic/room")
  public void getRooms(Principal principal) {
    String rooms = String.join(",", roomAndUsers.keySet());
    Response response = new Response("Admin: Existed rooms: {" + rooms +
        "}. Create your room by entering an inexistent number");
    messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/admin", response);
  }

  @MessageMapping("/message")
  public void sendMessage(Principal principal, Message message) throws Exception {
    Thread.sleep(1000);
    String room = userAndRoom.get(principal.getName());
    Set<String> users = roomAndUsers.getOrDefault(room, new HashSet<>());
    String name = userAndName.get(principal.getName());

    users.forEach(t -> {
      messagingTemplate.convertAndSendToUser(t, "/queue/chat", new Response(name + ": " + message.getContent()));
    });
  }

  private void login(Principal principal, Message message) throws Exception {
    Thread.sleep(1000);
    userAndRoom.put(principal.getName(), message.getRoom());
    Set<String> users = roomAndUsers.getOrDefault(message.getRoom(), new HashSet<>());
    users.add(principal.getName());
    roomAndUsers.put(message.getRoom(), users);
    userAndName.put(principal.getName(), message.getName());

    users.forEach(t -> {
      messagingTemplate.convertAndSendToUser(t, "/queue/chat",
          new Response("Admin: Welcome to " + message.getRoom() + "! " + message.getName()));
    });
  }

  private void logout(Principal principal) throws Exception {
    Thread.sleep(1000);
    String room = userAndRoom.get(principal.getName());
    Set<String> users = roomAndUsers.getOrDefault(room, new HashSet<>());
    String name = userAndName.get(principal.getName());
    users.forEach(t -> {
      messagingTemplate.convertAndSendToUser(t, "/queue/chat",
          new Response("Admin: " + name + " had left."));
    });
    userAndRoom.remove(principal.getName());
    userAndName.remove(principal.getName());
    users.remove(principal.getName());
  }

  @EventListener
  public void handle(SessionDisconnectEvent event) throws Exception {
    Principal user = event.getUser();
    logout(user);
  }

  @EventListener
  public void handle(SessionSubscribeEvent event) throws Exception {
    StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
    if ("/user/queue/chat".equals(headers.getDestination())) {
      Message message = new Message();
      message.setName(headers.getNativeHeader("name").get(0));
      message.setRoom(headers.getNativeHeader("room").get(0));
      login(headers.getUser(), message);
    }
  }
}
