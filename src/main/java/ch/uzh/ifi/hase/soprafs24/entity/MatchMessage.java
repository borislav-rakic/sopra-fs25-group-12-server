package ch.uzh.ifi.hase.soprafs24.entity;

import java.time.Instant;
import javax.persistence.*;

import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;

@Entity
public class MatchMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Match match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchMessageType type;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int seenByBitmask = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
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

    public int getSeenByBitmask() {
        return seenByBitmask;
    }

    public void setSeenByBitmask(int seenByBitmask) {
        this.seenByBitmask = seenByBitmask;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // --- Convenience methods for bitmask operations ---

    public void markSeen(int playerSlot) {
        this.seenByBitmask |= (1 << playerSlot);
    }

    public boolean hasSeen(int playerSlot) {
        return (this.seenByBitmask & (1 << playerSlot)) != 0;
    }
}
