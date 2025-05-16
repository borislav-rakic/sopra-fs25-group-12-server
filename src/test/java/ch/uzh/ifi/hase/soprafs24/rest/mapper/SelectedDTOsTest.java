package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;
import ch.uzh.ifi.hase.soprafs24.entity.User;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SelectedDTOsTest {

    @Test
    void testPlayerDTO() {
        PlayerDTO dto = new PlayerDTO();
        dto.setUserId(1L);
        dto.setUsername("Alice");
        dto.setIsAIPlayer(true);

        assertEquals(1L, dto.getUserId());
        assertEquals("Alice", dto.getUsername());
        assertTrue(dto.getIsAIPlayer());
    }

    @Test
    void testGameResultDTO() {
        GameResultDTO.PlayerScore score = new GameResultDTO.PlayerScore();
        score.setUsername("Bob");
        score.setTotalScore(42);
        score.setPointsThisGame(7);

        GameResultDTO result = new GameResultDTO();
        result.setMatchId(10L);
        result.setGameNumber(2);
        result.setPlayerScores(List.of(score));

        assertEquals(10L, result.getMatchId());
        assertEquals(2, result.getGameNumber());
        assertEquals(1, result.getPlayerScores().size());
        assertEquals("Bob", result.getPlayerScores().get(0).getUsername());
        assertEquals(42, result.getPlayerScores().get(0).getTotalScore());
        assertEquals(7, result.getPlayerScores().get(0).getPointsThisGame());
    }

    @Test
    void testTrickDTOAndTrickCard() {
        TrickDTO.TrickCard card = new TrickDTO.TrickCard();
        card.setCode("QH");
        card.setPosition(1);
        card.setOrder(2);

        TrickDTO dto = new TrickDTO();
        dto.setCards(List.of(card));
        dto.setTrickLeaderPosition(0);
        dto.setWinningPosition(1);

        assertEquals(1, dto.getCards().size());
        assertEquals("QH", dto.getCards().get(0).getCode());
        assertEquals(1, dto.getCards().get(0).getPosition());
        assertEquals(2, dto.getCards().get(0).getOrder());
        assertEquals(0, dto.getTrickLeaderPosition());
        assertEquals(1, dto.getWinningPosition());
    }

    @Test
    void testTrickDTOConstructor() {
        TrickDTO.TrickCard card = new TrickDTO.TrickCard("AS", 2, 3);
        TrickDTO dto = new TrickDTO(List.of(card), 1, 0);

        assertEquals("AS", dto.getCards().get(0).getCode());
        assertEquals(2, dto.getCards().get(0).getPosition());
        assertEquals(3, dto.getCards().get(0).getOrder());
        assertEquals(1, dto.getTrickLeaderPosition());
        assertEquals(0, dto.getWinningPosition());
    }

    @Test
    void testMatchMessageDTO() {
        String messageContent = "Some test content";

        MatchMessage entity = new MatchMessage();
        entity.setId(123L);
        entity.setType(MatchMessageType.LAST_TRICK_STARTED);
        entity.setContent(messageContent);
        Instant now = Instant.now();
        entity.setCreatedAt(now);

        MatchMessageDTO dto = new MatchMessageDTO(entity);

        assertEquals("123", dto.getId());
        assertEquals(MatchMessageType.LAST_TRICK_STARTED, dto.getType());
        assertEquals(messageContent, dto.getContent()); // <- safer
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        MessageDTO dto = new MessageDTO();
        dto.setMessage("Test message");

        assertEquals("Test message", dto.getMessage());
    }

    @Test
    void testConstructorMessageDTO() {
        MessageDTO dto = new MessageDTO("Hello, world!");

        assertEquals("Hello, world!", dto.getMessage());
    }

    @Test
    void testMatchMessageDTO_withNullFields() {
        MatchMessage entity = new MatchMessage(); // all fields null
        MatchMessageDTO dto = new MatchMessageDTO(entity);

        assertEquals("0", dto.getId());
        assertEquals(MatchMessageType.GAME_STARTED, dto.getType());
        assertEquals("", dto.getContent());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void testPlayerCardDTOGettersAndSetters() {
        PlayerCardDTO dto = new PlayerCardDTO();

        dto.setGameId(101L);
        dto.setGameNumber(3);
        dto.setPlayerId(42L);
        dto.setCard("QS");

        assertEquals(101L, dto.getGameId());
        assertEquals(3, dto.getGameNumber());
        assertEquals(42L, dto.getPlayerId());
        assertEquals("QS", dto.getCard().getCode());
        assertEquals(62, dto.getCardOrder()); // Q=12;S=50;QS=62
    }

    @Test
    void testConstructorGetDTO() {
        Long id = 1L;
        String username = "testUser";
        UserStatus status = UserStatus.ONLINE;
        int avatar = 3;
        LocalDate birthday = LocalDate.of(2000, 1, 1);
        int scoreTotal = 100;
        int gamesPlayed = 20;
        int avgGameRanking = 2;
        int avgMatchRanking = 1;
        int moonShots = 5;
        int perfectGames = 2;
        int perfectMatches = 1;
        int currentGameStreak = 3;
        int longestGameStreak = 6;
        int currentMatchStreak = 2;
        int longestMatchStreak = 4;
        boolean isAiPlayer = true;
        boolean isGuest = false;

        UserGetDTO dto = new UserGetDTO(id, username, status, avatar, birthday, scoreTotal,
                gamesPlayed, avgGameRanking, avgMatchRanking, moonShots, perfectGames,
                perfectMatches, currentGameStreak, longestGameStreak,
                currentMatchStreak, longestMatchStreak, isAiPlayer, isGuest);

        assertEquals(id, dto.getId());
        assertEquals(username, dto.getUsername());
        assertEquals(status, dto.getStatus());
        assertEquals(avatar, dto.getAvatar());
        assertEquals(birthday, dto.getBirthday());
        assertEquals(scoreTotal, dto.getScoreTotal());
        assertEquals(gamesPlayed, dto.getGamesPlayed());
        assertEquals(avgGameRanking, dto.getAvgGameRanking());
        assertEquals(avgMatchRanking, dto.getAvgMatchRanking());
        assertEquals(moonShots, dto.getMoonShots());
        assertEquals(perfectGames, dto.getPerfectGames());
        assertEquals(perfectMatches, dto.getPerfectMatches());
        assertEquals(currentGameStreak, dto.getCurrentGameStreak());
        assertEquals(longestGameStreak, dto.getLongestGameStreak());
        assertEquals(currentMatchStreak, dto.getCurrentMatchStreak());
        assertEquals(longestMatchStreak, dto.getLongestMatchStreak());
        assertTrue(dto.isAiPlayer());
        assertFalse(dto.isGuest());
    }

    @Test
    void testSetPlayer1Id_validUser() {
        MatchDTO dto = new MatchDTO();
        User user = new User();
        user.setId(100L);

        dto.setPlayer1Id(user);
        assertEquals(100L, dto.getPlayer1Id());
    }

    @Test
    void testSetPlayer2Id_validUser() {
        MatchDTO dto = new MatchDTO();
        User user = new User();
        user.setId(200L);

        dto.setPlayer2Id(user);
        assertEquals(200L, dto.getPlayer2Id());
    }

    @Test
    void testSetPlayer3Id_validUser() {
        MatchDTO dto = new MatchDTO();
        User user = new User();
        user.setId(300L);

        dto.setPlayer3Id(user);
        assertEquals(300L, dto.getPlayer3Id());
    }

    @Test
    void testSetPlayer4Id_validUser() {
        MatchDTO dto = new MatchDTO();
        User user = new User();
        user.setId(400L);

        dto.setPlayer4Id(user);
        assertEquals(400L, dto.getPlayer4Id());
    }

    @Test
    void testSetPlayer1Id_nullUser() {
        MatchDTO dto = new MatchDTO();

        dto.setPlayer1Id((User) null);
        assertNull(dto.getPlayer1Id());
    }

    @Test
    void testSetPlayer2Id_nullUser() {
        MatchDTO dto = new MatchDTO();

        dto.setPlayer2Id((User) null);
        assertNull(dto.getPlayer2Id());
    }

    @Test
    void testSetPlayer3Id_nullUser() {
        MatchDTO dto = new MatchDTO();

        dto.setPlayer3Id((User) null);
        assertNull(dto.getPlayer3Id());
    }

    @Test
    void testSetPlayer4Id_nullUser() {
        MatchDTO dto = new MatchDTO();

        dto.setPlayer4Id((User) null);
        assertNull(dto.getPlayer4Id());
    }
}
