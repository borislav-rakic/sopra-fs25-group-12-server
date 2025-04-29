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
                matchPlayer.setUser(new User());
                matchPlayer.setMatch(match);
                matchPlayers.add(matchPlayer);

                match.setMatchId(1L);
                match.setStarted(false);
                match.setMatchPlayers(matchPlayers);
                match.setHostId(4L);
                match.setMatchGoal(100);
                match.setInvites(new HashMap<>());
                match.setAiPlayers(new HashMap<>());
                match.setPlayer1(new User());

                List<Long> matchPlayerIds = new ArrayList<>();
                matchPlayerIds.add(match.getMatchPlayers().get(0).getMatchPlayerId());

                // mock the service to return the expected match
                given(matchService.createNewMatch(Mockito.any())).willReturn(match);

                MockHttpServletRequestBuilder postRequest = post("/matches")
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
                matchPlayer.setUser(new User());
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
        public void testGetPolling() throws Exception {
                Match match = new Match();

                List<MatchPlayer> matchPlayers = new ArrayList<>();
                MatchPlayer matchPlayer = new MatchPlayer();
                matchPlayer.setUser(new User());
                matchPlayer.setMatch(match);
                matchPlayers.add(matchPlayer);

                match.setMatchId(1L);
                match.setStarted(false);
                match.setMatchPlayers(matchPlayers);

                List<Long> matchPlayerIds = new ArrayList<>();
                matchPlayerIds.add(match.getMatchPlayers().get(0).getMatchPlayerId());

                given(matchService.getPolling(Mockito.any())).willReturn(match);

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
        public void testUpdateMatchGoal() throws Exception {
                Map<String, Integer> body = new HashMap<>();
                body.put("matchGoal", 150);

                doNothing().when(matchService).updateMatchGoal(Mockito.any(), Mockito.any());

                MockHttpServletRequestBuilder postRequest = post("/matches/1/matchGoal")
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

        @Test
        public void testGetPlayerPolling() throws Exception {
                PollingDTO playerPollingDTO = new PollingDTO();
                playerPollingDTO.setMatchId(1L);

                Map<Integer, Integer> aiPlayers = new HashMap<>();
                aiPlayers.put(1, 1); // Player 1 - Easy
                aiPlayers.put(2, 2); // Player 2 - Medium
                aiPlayers.put(3, 3); // Player 3 - Hard
                playerPollingDTO.setAiPlayers(aiPlayers);

                List<String> matchPlayers = new ArrayList<>();
                matchPlayers.add("User");

                playerPollingDTO.setMatchPlayers(matchPlayers);
                playerPollingDTO.setHostId(4L);
                playerPollingDTO.setMatchGoal(100);

                given(matchService.getPlayerPolling(Mockito.any(), Mockito.any()))
                                .willReturn(playerPollingDTO);

                MockHttpServletRequestBuilder postRequest = post("/matches/1/logic")
                                .header("Authorization", "Bearer 1234");

                mockMvc.perform(postRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.matchId", is(playerPollingDTO.getMatchId().intValue())))
                                .andExpect(jsonPath("$.matchPlayers", is(playerPollingDTO.getMatchPlayers())))
                                .andExpect(jsonPath("$.aiPlayers.1", is(1)))
                                .andExpect(jsonPath("$.aiPlayers.2", is(2)))
                                .andExpect(jsonPath("$.aiPlayers.3", is(3)))
                                .andExpect(jsonPath("$.matchGoal", is(playerPollingDTO.getMatchGoal())))
                                .andExpect(jsonPath("$.hostId", is(playerPollingDTO.getHostId().intValue())));
        }
}
