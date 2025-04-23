package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.model.Card;

public class PlayerMatchInformationDTO {
    private Long matchId;
    private List<String> matchPlayers;
    private String host;
    private int length;
    private int slot;
    private Map<Integer, Integer> aiPlayers;
    private List<PlayerCardDTO> playerCards;
    private List<PlayerCardDTO> playableCards;
    private boolean isMyTurn = false;
    private GamePhase gamePhase;
    private MatchPhase matchPhase;
    private List<Card> currentTrick;
    private int trickLeaderSlot;
    private int lastTrickWinnerSlot;
    private int lastTrickPoints;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
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

    public List<Card> getCurrentTrick() {
        return currentTrick;
    }

    public void setCurrentTrick(List<Card> currentTrick) {
        this.currentTrick = currentTrick;
    }

    public int getTrickLeaderSlot() {
        return trickLeaderSlot;
    }

    public void setTrickLeaderSlot(int trickLeaderSlot) {
        this.trickLeaderSlot = trickLeaderSlot;
    }

    public int getLastTrickWinnerSlot() {
        return lastTrickWinnerSlot;
    }

    public void setLastTrickWinnerSlot(int lastTrickWinnerSlot) {
        this.lastTrickWinnerSlot = lastTrickWinnerSlot;
    }

    public int getLastTrickPoints() {
        return lastTrickPoints;
    }

    public void setLastTrickPoints(int lastTrickPoints) {
        this.lastTrickPoints = lastTrickPoints;
    }
}
