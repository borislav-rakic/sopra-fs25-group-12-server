package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.service.MatchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MatchControllerTest
 * This is a WebMvcTest which allows to test the MatchController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the MatchController works.
 */
@WebMvcTest(MatchController.class)
public class MatchControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @Test
    public void testCreateMatch() throws Exception {
        Match match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(new User());
        matchPlayer.setMatch(match);
        matchPlayers.add(matchPlayer);

        match.setMatchId(1L);
        match.setStarted(false);
        match.setMatchPlayers(matchPlayers);
        match.setHost("User");
        match.setLength(100);
        match.setInvites(new HashMap<>());
        match.setAiPlayers(new ArrayList<>());
        match.setPlayer1(new User());

        List<Long> matchPlayerIds = new ArrayList<>();
        matchPlayerIds.add(match.getMatchPlayers().get(0).getMatchPlayerId());

        MatchCreateDTO matchCreateDTO = new MatchCreateDTO();
        matchCreateDTO.setPlayerToken("1234");

        given(matchService.createNewMatch(Mockito.any())).willReturn(match);

        MockHttpServletRequestBuilder postRequest = post("/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(matchCreateDTO))
                .header("Authorization", "Bearer 1234");

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$.started", is(match.getStarted())))
                .andExpect(jsonPath("$.matchPlayerIds", is(matchPlayerIds)));
    }

    @Test
    public void testGetMatchesInformation() throws Exception {
        Match match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(new User());
        matchPlayer.setMatch(match);
        matchPlayers.add(matchPlayer);

        match.setMatchId(1L);
        match.setStarted(false);
        match.setMatchPlayers(matchPlayers);

        List<Long> matchPlayerIds = new ArrayList<>();
        matchPlayerIds.add(match.getMatchPlayers().get(0).getMatchPlayerId());

        List<Match> matches = new ArrayList<>();
        matches.add(match);

        given(matchService.getMatchesInformation()).willReturn(matches);

        MockHttpServletRequestBuilder getRequest = get("/matches");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$[0].started", is(match.getStarted())))
                .andExpect(jsonPath("$[0].matchPlayerIds", is(matchPlayerIds)));
    }

    @Test
    public void testGetMatchInformation() throws Exception {
        Match match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(new User());
        matchPlayer.setMatch(match);
        matchPlayers.add(matchPlayer);

        match.setMatchId(1L);
        match.setStarted(false);
        match.setMatchPlayers(matchPlayers);

        List<Long> matchPlayerIds = new ArrayList<>();
        matchPlayerIds.add(match.getMatchPlayers().get(0).getMatchPlayerId());

        given(matchService.getMatchInformation(Mockito.any())).willReturn(match);

        MockHttpServletRequestBuilder getRequest = get("/matches/1");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", is(match.getMatchId().intValue())))
                .andExpect(jsonPath("$.started", is(match.getStarted())))
                .andExpect(jsonPath("$.matchPlayerIds", is(matchPlayerIds)));
    }

    @Test
    public void testDeleteMatch() throws Exception {
        MockHttpServletRequestBuilder deleteRequest = delete("/matches/1")
                .header("Authorization", "Bearer 1234");

        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void testInvitePlayerToMatch() throws Exception {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(0);
        inviteRequestDTO.setUserId(1L);

        MockHttpServletRequestBuilder postRequest = post("/matches/1/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(inviteRequestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void testRespondToInviteError() throws Exception {
        MockHttpServletRequestBuilder postRequest = post("/matches/1/invite/respond")
                .header("Authorization", "Bearer 1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(null));

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRespondToInviteSuccess() throws Exception {
        InviteResponseDTO inviteResponseDTO = new InviteResponseDTO();
        inviteResponseDTO.setAccepted(true);

        doNothing().when(matchService).respondToInvite(Mockito.any(), Mockito.any(), Mockito.any());

        MockHttpServletRequestBuilder postRequest = post("/matches/1/invite/respond")
                .header("Authorization", "Bearer 1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(inviteResponseDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdateMatchLength() throws Exception {
        Map<String, Integer> body = new HashMap<>();
        body.put("matchLength", 150);

        doNothing().when(matchService).updateMatchLength(Mockito.any(), Mockito.any());

        MockHttpServletRequestBuilder postRequest = post("/matches/1/length")
                .header("Authorization", "Bearer 1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(body));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void testAddAiPlayer() throws Exception {
        AIPlayerDTO aiPlayerDTO = new AIPlayerDTO();
        aiPlayerDTO.setDifficulty(1);

        doNothing().when(matchService).addAiPlayer(Mockito.any(), Mockito.any());

        MockHttpServletRequestBuilder postRequest = post("/matches/1/ai")
                .header("Authorization", "Bearer 1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(aiPlayerDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void testSendJoinRequest() throws Exception {
        JoinRequestDTO joinRequestDTO = new JoinRequestDTO();
        joinRequestDTO.setUserId(1L);

        doNothing().when(matchService).sendJoinRequest(Mockito.any(), Mockito.any());

        MockHttpServletRequestBuilder postRequest = post("/matches/1/join")
                .header("Authorization", "Bearer 1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(joinRequestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void testAcceptJoinRequest() throws Exception {
        JoinRequestDTO joinRequestDTO = new JoinRequestDTO();
        joinRequestDTO.setUserId(1L);

        doNothing().when(matchService).acceptJoinRequest(Mockito.any(), Mockito.any());

        MockHttpServletRequestBuilder postRequest = post("/matches/1/join/accept")
                .header("Authorization", "Bearer 1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(joinRequestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void testDeclineJoinRequest() throws Exception {
        JoinRequestDTO joinRequestDTO = new JoinRequestDTO();
        joinRequestDTO.setUserId(1L);

        doNothing().when(matchService).declineJoinRequest(Mockito.any(), Mockito.any());

        MockHttpServletRequestBuilder postRequest = post("/matches/1/join/decline")
                .header("Authorization", "Bearer 1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(joinRequestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void testGetJoinRequests() throws Exception {
        JoinRequestDTO joinRequestDTO = new JoinRequestDTO();
        joinRequestDTO.setUserId(1L);
        joinRequestDTO.setStatus("Accepted");

        List<JoinRequestDTO> joinRequestDTOs = new ArrayList<>();
        joinRequestDTOs.add(joinRequestDTO);

        given(matchService.getJoinRequests(Mockito.any())).willReturn(joinRequestDTOs);

        MockHttpServletRequestBuilder getRequest = get("/matches/1/joinRequests")
                .header("Authorization", "Bearer 1234");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is(joinRequestDTO.getStatus())))
                .andExpect(jsonPath("$[0].userId", is(joinRequestDTO.getUserId().intValue())));
    }

    /**
     * Helper Method to convert userPostDTO into a JSON string such that the input
     * can be processed
     * Input will look like this: {"playerToken": "1234"}
     *
     * @param object Input
     * @return string
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e));
        }
    }
}
