package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;

public class PollingDTO {
    // Info about the Match context
    private int pollCounter; // [0]
    private Long matchId; // [1]
    private int matchGoal; // [2]
    private Long hostId; // [3]
    private MatchPhase matchPhase; // [4]

    // Info about the game state
    private GamePhase gamePhase; // [11]
    private TrickPhase trickPhase; // [12]
    private boolean heartsBroken; // [13]

    private TrickDTO currentTrickDTO;
    private TrickDTO previousTrickDTO;
    private Integer currentPlayerSlot; // [15c]
    private int currentPlayOrder; // [15d]

    private String resultHtml; // [18b]
    private List<MatchMessageDTO> matchMessages; // [18c]

    // Info about the other players
    private List<String> matchPlayers; // [21]
    private List<String> avatarUrls; // [22]
    private Map<Integer, Integer> cardsInHandPerPlayer; // [23]
    private Map<Integer, Integer> playerPoints; // [24]
    private Map<Integer, Integer> aiPlayers; // [25]

    // Info about myself
    private int matchPlayerSlot; // [31a]
    private int playerSlot; // [31b]
    private boolean isMyTurn = false; // [32]
    private List<PlayerCardDTO> playerCards; // [33a]
    private String playerCardsAsString; // [33b]
    private List<PlayerCardDTO> playableCards; // [34a]
    private String playableCardsAsString; // [34b]
    private String passingInfo; // [34c]
    private Integer passingToPlayerSlot; // [34d]

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

    public int getCurrentPlayOrder() {
        return currentPlayOrder;
    }

    public void setCurrentPlayOrder(int currentPlayOrder) {
        this.currentPlayOrder = currentPlayOrder;
    }

    public int getMatchPlayerSlot() {
        return matchPlayerSlot;
    }

    public void setMatchPlayerSlot(int matchPlayerSlot) {
        this.matchPlayerSlot = matchPlayerSlot;
        this.playerSlot = this.matchPlayerSlot - 1;
    }

    public int getPlayerSlot() {
        return playerSlot;
    }

    public void setPlayerSlot(int playerSlot) {
        this.playerSlot = playerSlot;
    }

    public int getPollCounter() {
        return pollCounter;
    }

    public void setPollCounter(int pollCounter) {
        this.pollCounter = pollCounter;
    }

    public Integer getCurrentPlayerSlot() {
        return currentPlayerSlot;
    }

    public void setCurrentPlayerSlot(Integer currentPlayerSlot) {
        this.currentPlayerSlot = currentPlayerSlot;
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

    public String getPlayableCardsAsString() {
        return playableCardsAsString;
    }

    public void setPlayableCardsAsString(String playableCardsAsString) {
        this.playableCardsAsString = playableCardsAsString;
    }

    public String getResultHtml() {
        return resultHtml;
    }

    public void setResultHtml(String resultHtml) {
        this.resultHtml = resultHtml;
    }

    public String getPlayerCardsAsString() {
        return playerCardsAsString;
    }

    public void setPlayerCardsAsString(String playerCardsAsString) {
        this.playerCardsAsString = playerCardsAsString;
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

    public Integer getPassingToPlayerSlot() {
        return passingToPlayerSlot;
    }

    public void setPassingToPlayerSlot(Integer passingToPlayerSlot) {
        this.passingToPlayerSlot = passingToPlayerSlot;
    }

    /******************** PREVIOUS TRICK **************************/

    public TrickDTO getCurrentTrickDTO() {
        return currentTrickDTO;
    }

    public void setCurrentTrickDTO(TrickDTO currentTrickDTO) {
        this.currentTrickDTO = currentTrickDTO;
    }

    public TrickDTO getPreviousTrickDTO() {
        return previousTrickDTO;
    }

    public void setPreviousTrickDTO(TrickDTO previousTrickDTO) {
        this.previousTrickDTO = previousTrickDTO;
    }

    /******************** PREVIOUS TRICK **************************/

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

    public TrickPhase getTrickPhase() {
        return trickPhase;
    }

    public void setTrickPhase(TrickPhase trickPhase) {
        this.trickPhase = trickPhase;
    }

    public List<MatchMessageDTO> getMatchMessages() {
        return matchMessages;
    }

    public void setMatchMessages(List<MatchMessageDTO> matchMessages) {
        this.matchMessages = matchMessages;
    }

    public String getPassingInfo() {
        return passingInfo;
    }

    public void setPassingInfo(String passingInfo) {
        this.passingInfo = passingInfo;
    }
}
