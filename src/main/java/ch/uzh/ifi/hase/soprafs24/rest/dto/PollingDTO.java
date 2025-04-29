package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.model.Card;

public class PollingDTO {
    // Info about the Match context
    private Long matchId; // [1]
    private int matchGoal; // [2]
    private Long hostId; // [3]
    private MatchPhase matchPhase; // [4]

    // Info about the game state
    private GamePhase gamePhase; // [11]
    private boolean trickInProgress; // [12]
    private boolean heartsBroken; // [13]

    private List<Card> currentTrick; // [14]
    private int currentTrickLeaderSlot; // [15]

    private List<Card> previousTrick; // [16]
    private int previousTrickWinnerSlot; // [17]
    private int previousTrickPoints; // [18]

    // Info about the other players
    private List<String> matchPlayers; // [21]
    private List<String> avatarUrls; // [22]
    private Map<Integer, Integer> cardsInHandPerPlayer; // [23]
    private Map<Integer, Integer> playerPoints; // [24]
    private Map<Integer, Integer> aiPlayers; // [25]
    // Info about myself
    private int slot; // [31]
    private boolean isMyTurn = false; // [32]
    private List<PlayerCardDTO> playerCards; // [33]
    private List<PlayerCardDTO> playableCards; // [34]

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public List<String> getMatchPlayers() {
        return matchPlayers;
    }

    public void setMatchPlayers(List<String> matchPlayers) {
        this.matchPlayers = matchPlayers;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public int getMatchGoal() {
        return matchGoal;
    }

    public void setMatchGoal(int matchGoal) {
        this.matchGoal = matchGoal;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public Map<Integer, Integer> getAiPlayers() {
        return aiPlayers;
    }

    public void setAiPlayers(Map<Integer, Integer> aiPlayers) {
        this.aiPlayers = aiPlayers;
    }

    public List<PlayerCardDTO> getPlayerCards() {
        return playerCards;
    }

    public void setPlayerCards(List<PlayerCardDTO> playerCards) {
        this.playerCards = playerCards;
    }

    public List<PlayerCardDTO> getPlayableCards() {
        return playableCards;
    }

    public void setPlayableCards(List<PlayerCardDTO> playableCards) {
        this.playableCards = playableCards;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public void setMyTurn(boolean myTurn) {
        this.isMyTurn = myTurn;
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

    public MatchPhase getMatchPhase() {
        return matchPhase;
    }

    public void setMatchPhase(MatchPhase matchPhase) {
        this.matchPhase = matchPhase;
    }

    public List<Card> getPreviousTrick() {
        return previousTrick;
    }

    public void setPreviousTrick(List<Card> previousTrick) {
        this.previousTrick = previousTrick;
    }

    public List<Card> getCurrentTrick() {
        return currentTrick;
    }

    public void setCurrentTrick(List<Card> currentTrick) {
        this.currentTrick = currentTrick;
    }

    public int getCurrentTrickLeaderSlot() {
        return currentTrickLeaderSlot;
    }

    public void setCurrentTrickLeaderSlot(int currentTrickLeaderSlot) {
        this.currentTrickLeaderSlot = currentTrickLeaderSlot;
    }

    public int getPreviousTrickWinnerSlot() {
        return previousTrickWinnerSlot;
    }

    public void setPreviousTrickWinnerSlot(int previousTrickWinnerSlot) {
        this.previousTrickWinnerSlot = previousTrickWinnerSlot;
    }

    public int getPreviousTrickPoints() {
        return previousTrickPoints;
    }

    public void setPreviousTrickPoints(int previousTrickPoints) {
        this.previousTrickPoints = previousTrickPoints;
    }

    public boolean isHeartsBroken() {
        return heartsBroken;
    }

    public void setHeartsBroken(boolean heartsBroken) {
        this.heartsBroken = heartsBroken;
    }

    public Map<Integer, Integer> getPlayerPoints() {
        return playerPoints;
    }

    public void setPlayerPoints(Map<Integer, Integer> playerPoints) {
        this.playerPoints = playerPoints;
    }

    public Map<Integer, Integer> getCardsInHandPerPlayer() {
        return cardsInHandPerPlayer;
    }

    public void setCardsInHandPerPlayer(Map<Integer, Integer> cardsInHandPerPlayer) {
        this.cardsInHandPerPlayer = cardsInHandPerPlayer;
    }

    public List<String> getAvatarUrls() {
        return avatarUrls;
    }

    public void setAvatarUrls(List<String> avatarUrls) {
        this.avatarUrls = avatarUrls;
    }

    public boolean isTrickInProgress() {
        return trickInProgress;
    }

    public void setTrickInProgress(boolean trickInProgress) {
        this.trickInProgress = trickInProgress;
    }

}
