package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
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

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "host_username")
    private String hostUsername;

    @Column(name = "matchGoal")
    private int matchGoal;

    @Column(nullable = false)
    private boolean ready = false;

    @Column
    private boolean started;

    @ElementCollection
    @CollectionTable(name = "match_invites", joinColumns = @JoinColumn(name = "match_id"))
    @MapKeyColumn(name = "matchPlayerSlot_index")
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

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Game> games = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PassedCard> passedCards = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MatchMessage> messages = new ArrayList<>();

    @OneToOne(cascade = CascadeType.PERSIST) // Automatically persist MatchSummary when saving Match
    @JoinColumn(name = "match_summary_id")
    private MatchSummary matchSummary;

    @Column(nullable = false)
    private boolean fastForwardMode = false;

    @Column(name = "match_scores_csv")
    private String matchScoresCsv = "0,0,0,0"; // Example: "4,5,3,13"

    public void setFastForwardMode(boolean fastForwardMode) {
        this.fastForwardMode = fastForwardMode;
    }

    public boolean getFastForwardMode() {
        return fastForwardMode;
    }

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

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setMatchGoal(int matchGoal) {
        this.matchGoal = matchGoal;
    }

    public int getMatchGoal() {
        return matchGoal;
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

    public List<PassedCard> getPassedCards() {
        return passedCards;
    }

    public void setPassedCards(List<PassedCard> passedCards) {
        this.passedCards = passedCards;
    }

    public List<MatchMessage> getMessages() {
        return messages;
    }

    public void addMessage(MatchMessage message) {
        this.messages.add(message);
        message.setMatch(this); // ensures both sides of the relationship are in sync
    }

    // ======== MATCH SCORE ============ //

    @Transient
    public List<Integer> getMatchScoresList() {
        if (matchScoresCsv == null || matchScoresCsv.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(matchScoresCsv.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public void setMatchScoresList(List<Integer> scores) {
        this.matchScoresCsv = scores.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public int getScoreForSlot(int matchPlayerSlot) {
        List<Integer> scores = getMatchScoresList();
        if (matchPlayerSlot < 1 || matchPlayerSlot > scores.size()) {
            throw new IllegalArgumentException("Invalid player slot: " + matchPlayerSlot);
        }
        return scores.get(matchPlayerSlot - 1);
    }

    // ======== SOME HELPERS =========== //

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

    public int requireSlotByUser(User user) {
        if (player1 != null && player1.getId().equals(user.getId()))
            return 1;
        if (player2 != null && player2.getId().equals(user.getId()))
            return 2;
        if (player3 != null && player3.getId().equals(user.getId()))
            return 3;
        if (player4 != null && player4.getId().equals(user.getId()))
            return 4;
        throw new IllegalArgumentException("User is not part of this match.");
    }

    public MatchPlayer requireMatchPlayerByUser(User user) {
        Long userId = user.getId();
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        return matchPlayers.stream()
                .filter(mp -> mp.getUser() != null && userId.equals(mp.getUser().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User " + userId + " is not part of this match."));
    }

    public boolean containsPlayer(Long userId) {
        return (player1 != null && player1.getId().equals(userId)) ||
                (player2 != null && player2.getId().equals(userId)) ||
                (player3 != null && player3.getId().equals(userId)) ||
                (player4 != null && player4.getId().equals(userId));
    }

    public MatchPhase getPhase() {
        return phase;
    }

    public void setPhase(MatchPhase phase) {
        this.phase = phase;
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    public MatchSummary getMatchSummary() {
        return matchSummary;
    }

    public void setMatchSummary(MatchSummary matchSummary) {
        this.matchSummary = matchSummary;
    }

    // ==== UTIL FUNCTIONS

    public User requireUserBySlot(int matchPlayerSlot) {
        return matchPlayers.stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot)
                .map(MatchPlayer::getUser)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player in matchPlayerSlot " + matchPlayerSlot));
    }

    public Long rqequireUserIdBySlot(int matchPlayerSlot) {
        return matchPlayers.stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot)
                .map(mp -> mp.getUser().getId())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player in matchPlayerSlot " + matchPlayerSlot));
    }

    public Game getActiveGameOrThrow() {
        return this.getGames().stream()
                .filter(game -> game.getPhase() != GamePhase.FINISHED && game.getPhase() != GamePhase.ABORTED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active game found for this match (Match)."));
    }

    public Game getActiveGame() {
        return this.getGames().stream()
                .filter(game -> game.getPhase() != GamePhase.FINISHED && game.getPhase() != GamePhase.ABORTED)
                .findFirst()
                .orElse(null);
    }

    public MatchPlayer requireMatchPlayerBySlot(int matchPlayerSlot) {
        return this.getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No player found for matchPlayerSlot: " + matchPlayerSlot));
    }

    public List<MatchPlayer> findPlayersExceptSlot(int excludedSlot) {
        return this.getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() != excludedSlot)
                .toList();
    }

    public Map<Integer, Integer> collectCurrentGameScores() {
        Map<Integer, Integer> pointsOfPlayers = new HashMap<>();
        for (MatchPlayer mp : this.getMatchPlayers()) {
            pointsOfPlayers.put(mp.getMatchPlayerSlot(), mp.getGameScore());
        }
        return pointsOfPlayers;
    }

    public void addGame(Game game) {
        if (this.games == null) {
            this.games = new ArrayList<>();
        }
        this.games.add(game);
        game.setMatch(this); // ensure both sides consistent
    }

    public MatchPlayer requireMatchPlayerByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or blank.");
        }

        return this.matchPlayers.stream()
                .filter(mp -> mp.getUser() != null && token.equals(mp.getUser().getToken()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No MatchPlayer found with token: " + token));
    }

    public MatchPlayer requireHostPlayer() {
        return this.matchPlayers.stream()
                .filter(MatchPlayer::getIsHost)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No host player found in match " + this.matchId));
    }

}
