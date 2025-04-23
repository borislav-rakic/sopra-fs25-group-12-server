package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import ch.uzh.ifi.hase.soprafs24.entity.Game;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;

@Entity
@Table(name = "MATCH")
public class Match implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchId;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MatchPlayer> matchPlayers = new ArrayList<>();

    @Column(name = "host")
    private String host;

    @Column(name = "length")
    private int length;

    @Column
    private boolean started;

    @ElementCollection
    @CollectionTable(name = "match_invites", joinColumns = @JoinColumn(name = "match_id"))
    @MapKeyColumn(name = "slot_index")
    @Column(name = "user_id")
    private Map<Integer, Long> invites = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "match_ai_players", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "difficulty")
    private Map<Integer, Integer> aiPlayers = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "match_join_requests", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "user_id")
    private Map<Long, String> joinRequests = new HashMap<>();

    @Column(name = "deck_id")
    private String deckId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchPhase phase = MatchPhase.SETUP;

    @ManyToOne
    @JoinColumn(name = "player_1")
    private User player1;

    @ManyToOne
    @JoinColumn(name = "player_2")
    private User player2;

    @ManyToOne
    @JoinColumn(name = "player_3")
    private User player3;

    @ManyToOne
    @JoinColumn(name = "player_4")
    private User player4;

    @Column
    private int currentSlot;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Game> games = new ArrayList<>();

    public List<Game> getGames() {
        return games;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }

    public Map<Long, String> getJoinRequests() {
        return joinRequests;
    }

    public void setJoinRequests(Map<Long, String> joinRequests) {
        this.joinRequests = joinRequests;
    }

    public Map<Integer, Integer> getAiPlayers() {
        return aiPlayers;
    }

    public void setAiPlayers(Map<Integer, Integer> aiDifficulties) {
        this.aiPlayers = aiDifficulties;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public List<MatchPlayer> getMatchPlayers() {
        return matchPlayers;
    }

    public void setMatchPlayers(List<MatchPlayer> matchPlayers) {
        this.matchPlayers = matchPlayers;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean getStarted() {
        return started;
    }

    public Map<Integer, Long> getInvites() {
        return invites;
    }

    public void setInvites(Map<Integer, Long> invites) {
        this.invites = invites;
    }

    public String getJoinRequestStatus(Long userId) {
        return joinRequests.getOrDefault(userId, "not found");
    }

    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }

    public String getDeckId() {
        return deckId;
    }

    public User getPlayer1() {
        return player1;
    }

    public void setPlayer1(User player1) {
        this.player1 = player1;
    }

    public User getPlayer2() {
        return player2;
    }

    public void setPlayer2(User player2) {
        this.player2 = player2;
    }

    public User getPlayer3() {
        return player3;
    }

    public void setPlayer3(User player3) {
        this.player3 = player3;
    }

    public User getPlayer4() {
        return player4;
    }

    public void setPlayer4(User player4) {
        this.player4 = player4;
    }

    public int getSlotByPlayerId(Long playerId) {
        if (player1 != null && player1.getId().equals(playerId))
            return 1;
        if (player2 != null && player2.getId().equals(playerId))
            return 2;
        if (player3 != null && player3.getId().equals(playerId))
            return 3;
        if (player4 != null && player4.getId().equals(playerId))
            return 4;
        return -1; // Or throw an exception if player is not found
    }

    public boolean containsPlayer(Long userId) {
        return (player1 != null && player1.getId().equals(userId)) ||
                (player2 != null && player2.getId().equals(userId)) ||
                (player3 != null && player3.getId().equals(userId)) ||
                (player4 != null && player4.getId().equals(userId));
    }

    public int getCurrentSlot() {
        return currentSlot;
    }

    public void setCurrentSlot(int currentSlot) {
        this.currentSlot = currentSlot;
    }

    public MatchPhase getPhase() {
        return phase;
    }

    public void setPhase(MatchPhase phase) {
        this.phase = phase;
    }

    // ==== UTIL FUNCTIONS

    public User getUserBySlot(int slot) {
        return matchPlayers.stream()
                .filter(mp -> mp.getSlot() == slot)
                .map(MatchPlayer::getPlayerId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player in slot " + slot));
    }

}
