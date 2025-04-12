package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GameControllerTest
 * This is a WebMvcTest which allows to test the GameController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the GameController works.
 */
@WebMvcTest(GameController.class)
public class GameControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @Test
    public void testGetPlayerMatchInformation() throws Exception {
        PlayerMatchInformationDTO playerMatchInformationDTO = new PlayerMatchInformationDTO();
        playerMatchInformationDTO.setMatchId(1L);

        List<Integer> aiPlayers = new ArrayList<>();
        aiPlayers.add(1);
        aiPlayers.add(1);
        aiPlayers.add(1);

        playerMatchInformationDTO.setAiPlayers(aiPlayers);

        List<String> matchPlayers = new ArrayList<>();
        matchPlayers.add("User");

        playerMatchInformationDTO.setMatchPlayers(matchPlayers);
        playerMatchInformationDTO.setHost("User");
        playerMatchInformationDTO.setLength(100);
        playerMatchInformationDTO.setStarted(true);

        given(gameService.getPlayerMatchInformation(Mockito.any(), Mockito.any())).willReturn(playerMatchInformationDTO);

        MockHttpServletRequestBuilder postRequest = post("/matches/1/logic")
                .header("Authorization", "Bearer 1234");

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", is(playerMatchInformationDTO.getMatchId().intValue())))
                .andExpect(jsonPath("$.started", is(playerMatchInformationDTO.getStarted())))
                .andExpect(jsonPath("$.matchPlayers", is(playerMatchInformationDTO.getMatchPlayers())))
                .andExpect(jsonPath("$.aiPlayers", is(playerMatchInformationDTO.getAiPlayers())))
                .andExpect(jsonPath("$.length", is(playerMatchInformationDTO.getLength())))
                .andExpect(jsonPath("$.host", is(playerMatchInformationDTO.getHost())));
    }
}
