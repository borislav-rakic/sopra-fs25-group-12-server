package ch.uzh.ifi.hase.soprafs24.util;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;

@ExtendWith(MockitoExtension.class)
class DebuggingServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @BeforeEach
    void setup() throws Exception {
        // Inject mocks into static fields via reflection (hacky, but needed due to
        // @PostConstruct)
        Field matchField = DebuggingService.class.getDeclaredField("matchRepository");
        matchField.setAccessible(true);
        matchField.set(null, matchRepository);

        Field gameField = DebuggingService.class.getDeclaredField("gameRepository");
        gameField.setAccessible(true);
        gameField.set(null, gameRepository);

        Field statsField = DebuggingService.class.getDeclaredField("gameStatsRepository");
        statsField.setAccessible(true);
        statsField.set(null, gameStatsRepository);
    }

}
