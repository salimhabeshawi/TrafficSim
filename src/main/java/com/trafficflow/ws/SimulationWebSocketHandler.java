package com.trafficflow.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trafficflow.model.SimulationState;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimulationWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void broadcast(SimulationState state) {
        try {
            String json = mapper.writeValueAsString(state);
            TextMessage message = new TextMessage(json);
            sessions.forEach(s -> send(s, message));
        } catch (Exception e) {
            // JSON serialization failed, skip broadcast
        }
    }

    private void send(WebSocketSession session, TextMessage message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(message);
            } catch (Exception e) {
                // Client disconnected or session error
            }
        }
    }
}
