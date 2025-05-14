package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;

@ExtendWith(MockitoExtension.class)
public class CardPassingServiceTest {

    @InjectMocks
    private CardPassingService cardPassingService;

    @Mock
    private AiPassingService aiPassingService;

    @Mock
    private CardRulesService cardRulesService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private PassedCardRepository passedCardRepository;

    private MatchPlayer matchPlayer;
    private Game game;
    private User user;
    private Match match;

    @BeforeEach
    void setup() {

        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(4L);
        user.setToken("1234");

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatchPlayerSlot(1);
        matchPlayer.setHand("2C,3H,4D");
        matchPlayer.setUser(new User());

        match = new Match();
        match.setMatchPlayers(List.of(matchPlayer));
        matchPlayer.setMatch(match);

        game = new Game();
        game.setGameId(42L);
        game.setMatch(match);
        game.setGameNumber(1);
    }

    // @Test
    // public void testCollectPassedCards() {
    // Game game = mock(Game.class);
    // Match match = mock(Match.class);
    // when(game.getMatch()).thenReturn(match);

    // List<PassedCard> passedCards = new ArrayList<>();
    // PassedCard card = mock(PassedCard.class);
    // passedCards.add(card);

    // when(passedCardRepository.findByGame(game)).thenReturn(passedCards);
    // when(cardRulesService.determinePassingDirection(game.getGameNumber())).thenReturn(new
    // HashMap<>());

    // cardPassingService.collectPassedCards(game);

    // verify(passedCardRepository, times(1)).deleteAll(passedCards);
    // }

    @Test
    void testPassingValidCards_successfullySavesCards() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H", "4D"));

        // All cards are valid, owned, and not passed
        for (String card : dto.getCards()) {
            when(passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(game, 1, card))
                    .thenReturn(false);
            when(passedCardRepository.existsByGameAndRankSuit(game, card)).thenReturn(false);
        }

        // Act
        cardPassingService.passingAcceptCards(game, matchPlayer, dto, false);

        // Assert
        verify(passedCardRepository).saveAll(anyList());
        verify(passedCardRepository).flush();
    }

    @Test
    void testPassingFailsWithTooFewCards() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H"));

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testPassingFailsWithDuplicateCards() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "2C", "3H"));

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testPassingFailsWithInvalidFormat() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "X1", "3H"));

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testPassingFailsWhenCardNotOwned() {
        matchPlayer.setHand("2C,3H"); // No "4D"

        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H", "4D"));

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testPassingFailsWhenCardAlreadyPassedByPlayer() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H", "4D"));

        when(passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(
                eq(game), eq(1), eq("2C"))).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    public void testPlayerSlotToMatchPlayerSlot() {
        assertEquals(1, cardPassingService.playerSlotToMatchPlayerSlot(0));
        assertEquals(4, cardPassingService.playerSlotToMatchPlayerSlot(3));
    }

    @Test
    public void testMatchPlayerSlotToPlayerSlot() {
        assertEquals(0, cardPassingService.matchPlayerSlotToPlayerSlot(1));
        assertEquals(3, cardPassingService.matchPlayerSlotToPlayerSlot(4));
    }

}
