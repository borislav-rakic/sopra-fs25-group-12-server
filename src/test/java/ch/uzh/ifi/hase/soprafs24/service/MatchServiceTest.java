package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayedCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class MatchServiceTest {

    @Mock
    private GameRepository gameRepository = Mockito.mock(GameRepository.class);

    @Mock
    private GameService gameService = Mockito.mock(GameService.class);

    @Mock
    private GameSimulationService gameSimulationService = Mockito.mock(GameSimulationService.class);

    @Mock
    private GameSetupService gameSetupService = Mockito.mock(GameSetupService.class);

    @Mock
    private MatchSummaryService matchSummaryService = Mockito.mock(MatchSummaryService.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private PollingService pollingService = Mockito.mock(PollingService.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @Mock
    private MatchSetupService matchSetupService = Mockito.mock(MatchSetupService.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @InjectMocks
    private MatchService matchService = new MatchService(
            gameRepository,
            gameService,
            gameSetupService,
            gameSimulationService,
            matchSummaryService,
            matchPlayerRepository,
            matchRepository,
            pollingService,
            userRepository,
            userService,
            matchSetupService
    // alphabetical order
    );

    private Match match;
    private Game game;
    private User user;
    private User user2;
    private MatchPlayer matchPlayer;
    private MatchPlayer matchPlayer2;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(11L); // Users up to id 10 are reserverd for AI Players.
        user.setToken("1234");
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        user2 = new User();
        user2.setUsername("username2");
        user2.setPassword("password2");
        user2.setId(12L); // Users up to id 10 are reserverd for AI Players.
        user2.setToken("12342");
        user2.setIsAiPlayer(false);
        user2.setStatus(UserStatus.ONLINE);

        match = new Match();
        match.setPhase(MatchPhase.SETUP);

        game = new Game();
        game.setGameId(1L);
        game.setMatch(match);

        match.getGames().add(game);

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setUser(user);
        matchPlayer.setMatchPlayerId(1L);
        matchPlayer.setMatchPlayerSlot(1);

        matchPlayer2 = new MatchPlayer();
        matchPlayer2.setMatch(match);
        matchPlayer2.setUser(user2);
        matchPlayer2.setMatchPlayerId(2L);
        matchPlayer2.setMatchPlayerSlot(2);

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(matchPlayer);
        matchPlayers.add(matchPlayer2);

        match.setMatchPlayers(matchPlayers);
        match.setHostId(user.getId());
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);
    }

    @Test
    public void testGetMatchesInformation() {
        List<Match> matchList = new ArrayList<>();
        matchList.add(match);

        when(matchRepository.findAll()).thenReturn(matchList);

        List<Match> result = matchService.getMatchesInformation();

        assertEquals(matchList, result);
    }

    @Test
    public void testGetMatchDTOError() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.getMatchDTO(1L),
                "Expected getMatchDTO to throw an exception");
    }

    @Test
    public void testGetMatchDTOSuccess() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        Match result = matchService.getMatchDTO(1L);

        assertEquals(match, result);
    }

    @Test
    public void testFindNewHumanHostOrAbortMatch() {
        given(userRepository.findUserById(Mockito.any())).willReturn(user);
        given(matchPlayerRepository.findByUserAndMatch(Mockito.any(), Mockito.any())).willReturn(matchPlayer);

        doNothing().when(gameService).relayMessageToMatchMessageService(Mockito.any(), Mockito.any(), Mockito.any());

        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.findNewHumanHostOrAbortMatch(match);

        verify(userRepository, times(2)).findUserById(Mockito.any());
        verify(matchPlayerRepository).findByUserAndMatch(Mockito.any(), Mockito.any());
        verify(gameService, times(3)).relayMessageToMatchMessageService(Mockito.any(), Mockito.any(), Mockito.any());
        verify(matchPlayerRepository, times(2)).save(Mockito.any());
        verify(matchRepository, times(2)).save(Mockito.any());
    }

    @Test
    public void testLeaveMatch() {
        User aiPlayer = new User();
        aiPlayer.setUsername("ai");
        aiPlayer.setIsAiPlayer(true);
        aiPlayer.setId(1L);

        MatchPlayer aiMatchPlayer = new MatchPlayer();
        aiMatchPlayer.setMatchPlayerId(3L);
        aiMatchPlayer.setMatchPlayerSlot(3);
        aiMatchPlayer.setUser(aiPlayer);

        given(matchRepository.findMatchByMatchId(Mockito.anyLong())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user2);
        given(userRepository.findUserById(Mockito.anyLong())).willReturn(aiPlayer);

        doNothing().when(gameService).relayMessageToMatchMessageService(Mockito.any(), Mockito.any(), Mockito.any());

        matchService.leaveMatch(1L, "12342", null);

        verify(matchRepository).findMatchByMatchId(Mockito.anyLong());
        verify(userRepository).findUserByToken(Mockito.any());
        verify(userRepository).findUserById(Mockito.anyLong());
        verify(gameService, times(2)).relayMessageToMatchMessageService(Mockito.any(), Mockito.any(), Mockito.any());
        verify(matchPlayerRepository).save(Mockito.any());
        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testPassingAcceptCardsSkipPassing() {
        game.setPhase(GamePhase.SKIP_PASSING);

        given(matchRepository.findById(Mockito.anyLong())).willReturn(Optional.ofNullable(match));
        given(userService.requireUserByToken(Mockito.any())).willReturn(user);
        given(gameRepository.findActiveGameByMatchId(Mockito.any())).willReturn(game);
        given(matchPlayerRepository.saveAndFlush(Mockito.any())).willReturn(matchPlayer);

        matchService.passingAcceptCards(1L, null, "1234", true);

        verify(matchRepository).findById(Mockito.anyLong());
        verify(userService).requireUserByToken(Mockito.any());
        verify(gameRepository).findActiveGameByMatchId(Mockito.any());
        verify(matchPlayerRepository).saveAndFlush(Mockito.any());
    }

    @Test
    public void testPlayCardAsHuman() {
        PlayedCardDTO playedCardDTO = new PlayedCardDTO();
        playedCardDTO.setCard("XX");

        given(matchRepository.findById(Mockito.anyLong())).willReturn(Optional.ofNullable(match));
        given(gameRepository.findActiveGameByMatchId(Mockito.any())).willReturn(game);

        doNothing().when(gameService).playCardAsHuman(Mockito.any(), Mockito.any(), Mockito.any());

        matchService.playCardAsHuman("1234", 1L, playedCardDTO);

        verify(matchRepository).findById(Mockito.anyLong());
        verify(gameRepository).findActiveGameByMatchId(Mockito.any());
        verify(gameService).playCardAsHuman(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testWrapUpCompletedGame() {
        given(matchSummaryService.buildMatchResultHtml(match, game)).willReturn("{test_content}");
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.wrapUpCompletedGame(game);

        verify(matchSummaryService).buildMatchResultHtml(match, game);
        verify(matchRepository, times(2)).save(Mockito.any());
    }

    @Test
    public void testGetPlayerPolling() {
        matchPlayer.setIsHost(true);
        matchPlayer.setMatchScore(200);

        match.setPhase(MatchPhase.IN_PROGRESS);

        game.setPhase(GamePhase.NORMALTRICK);
        game.setCurrentMatchPlayerSlot(1);
        game.setTrickPhase(TrickPhase.RUNNINGTRICK);

        given(matchRepository.findMatchByMatchId(Mockito.anyLong())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);
        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);
        given(gameRepository.save(Mockito.any())).willReturn(game);

        doNothing().when(gameService).advanceTrickPhaseIfOwnerPolling(Mockito.any());

        given(matchSummaryService.buildMatchResultHtml(Mockito.any(), Mockito.any())).willReturn("{test_content}");

        given(matchRepository.saveAndFlush(Mockito.any())).willReturn(match);

        given(gameService.playSingleAiTurn(Mockito.any(), Mockito.any(), Mockito.any())).willReturn(true);

        given(gameService.finalizeGameIfComplete(Mockito.any())).willReturn(true);
        given(matchRepository.save(Mockito.any())).willReturn(match);
        given(pollingService.getPlayerPollingForPostMatchPhase(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                .willReturn(new PollingDTO());

        matchService.getPlayerPolling("1234", 1L);

        verify(matchRepository).findMatchByMatchId(Mockito.anyLong());
        verify(userRepository).findUserByToken(Mockito.any());
        verify(matchPlayerRepository).save(Mockito.any());
        verify(gameService).advanceTrickPhaseIfOwnerPolling(Mockito.any());
        verify(gameRepository).save(Mockito.any());
        verify(matchSummaryService).buildMatchResultHtml(Mockito.any(), Mockito.any());
        verify(matchRepository).saveAndFlush(Mockito.any());
        verify(pollingService).getPlayerPollingForPostMatchPhase(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void testHandleConfirmedGame() {
        matchPlayer.setReady(true);
        matchPlayer2.setReady(true);

        given(gameRepository.save(Mockito.any())).willReturn(game);
        given(matchRepository.save(Mockito.any())).willReturn(match);
        given(gameSetupService.createAndStartGameForMatch(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(game);

        matchService.handleConfirmedGame(match, game);

        verify(gameRepository).save(Mockito.any());
        verify(matchRepository).save(Mockito.any());
        verify(gameSetupService).createAndStartGameForMatch(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
