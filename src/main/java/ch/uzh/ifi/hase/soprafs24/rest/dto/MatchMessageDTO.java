package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.Instant;

import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;

public class MatchMessageDTO {

    private String id;
    private MatchMessageType type;
    private String content;
    private Instant createdAt;

    public MatchMessageDTO(MatchMessage message) {
        this.id = message.getId() != null ? message.getId().toString() : "0";
        this.type = message.getType() != null ? message.getType() : MatchMessageType.GAME_STARTED;
        this.content = message.getContent() != null ? message.getContent() : "";
        this.createdAt = message.getCreatedAt() != null ? message.getCreatedAt() : Instant.now();
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MatchMessageType getType() {
        return type;
    }

    public void setType(MatchMessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
