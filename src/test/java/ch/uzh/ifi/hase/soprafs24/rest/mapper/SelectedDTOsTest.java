package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;

import org.junit.jupiter.api.Test;

import java.time.Instant;
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
    void testMatchMessageDTO_withNullFields() {
        MatchMessage entity = new MatchMessage(); // all fields null
        MatchMessageDTO dto = new MatchMessageDTO(entity);

        assertEquals("0", dto.getId());
        assertEquals(MatchMessageType.GAME_STARTED, dto.getType());
        assertEquals("", dto.getContent());
        assertNotNull(dto.getCreatedAt());
    }
}
