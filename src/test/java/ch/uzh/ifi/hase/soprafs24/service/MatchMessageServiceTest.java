package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.MatchMessageRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchMessageDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class MatchMessageServiceTest {

    @Mock
    private MatchMessageRepository matchMessageRepository;

    @InjectMocks
    private MatchMessageService matchMessageService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testMessages_returnsUnseenAndMarksSeen() {
        Match match = new Match();
        MatchPlayer player = new MatchPlayer();
        player.setMatchPlayerSlot(2);

        MatchMessage msg1 = new MatchMessage();
        msg1.setContent("Message 1");
        msg1.setMatch(match);
        msg1.setType(MatchMessageType.GAME_STARTED); // ensure not null

        MatchMessage msg2 = new MatchMessage();
        msg2.setContent("Message 2");
        msg2.setMatch(match);
        msg2.setType(MatchMessageType.HEARTS_BROKEN); // also not null

        // Not yet seen by slot 2
        when(matchMessageRepository.findByMatch(match)).thenReturn(List.of(msg1, msg2));

        List<MatchMessageDTO> result = matchMessageService.messages(match, null, player);

        assertEquals(2, result.size());
        verify(matchMessageRepository).saveAll(anyList());

        assertTrue(msg1.hasSeen(2));
        assertTrue(msg2.hasSeen(2));
    }

    @Test
    public void testMessages_returnsUnseenMessages() {
        // Arrange
        Match match = new Match();
        match.setMatchId(42L);

        MatchPlayer player = new MatchPlayer();
        player.setMatchPlayerSlot(2);

        MatchMessage message = new MatchMessage();
        message.setMatch(match); // Required
        message.setType(MatchMessageType.GAME_STARTED); // Required
        message.setContent("The Game has begun!"); // Required
        message.setSeenByBitmask(0); // Ensure fresh/unseen

        when(matchMessageRepository.findByMatch(match)).thenReturn(List.of(message));

        // Act
        List<MatchMessageDTO> result = matchMessageService.messages(match, null, player);

        // Assert
        assertEquals(1, result.size());
        assertEquals("The Game has begun!", result.get(0).getContent());
    }

    @Test
    public void testAddMessage_savesCorrectly() {
        Match match = new Match();
        MatchMessageType type = MatchMessageType.GAME_STARTED;

        matchMessageService.addMessage(match, type, "Let’s go!");

        verify(matchMessageRepository).save(argThat(msg -> msg.getMatch().equals(match) &&
                msg.getType() == type &&
                "Let’s go!".equals(msg.getContent())));
    }

    @Test
    public void testGetFunMessage_producesNonEmptyString() {
        for (MatchMessageType type : MatchMessageType.values()) {
            String message = matchMessageService.getFunMessage(type);
            // Only test types with defined fun messages
            if (!message.isBlank()) {
                assertNotNull(message);
                assertFalse(message.trim().isEmpty(), "Message should not be empty for type: " + type);
            }
        }
    }

    @Test
    public void testGetFunMessageWithUser() {
        String who = "Alice";
        String msg = matchMessageService.getFunMessage(MatchMessageType.PLAYER_JOINED, who);
        assertTrue(msg.contains("Alice"));
    }

    @Test
    void testGetFunMessageWithUser_playerLeft() {
        String who = "Bob";
        String message = matchMessageService.getFunMessage(MatchMessageType.PLAYER_LEFT, who);

        assertNotNull(message);
        assertTrue(message.contains("Bob"), "Expected the message to include the player's name");
    }

}
