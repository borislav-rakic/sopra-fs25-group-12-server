package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private final UserService userService;

    @Autowired
    public LeaderboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<LeaderboardDTO> getLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "rating") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "") String filter) {

        Sort.Direction direction = Sort.Direction.fromString(order);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        Page<User> users = userService.findUsersForLeaderboard(filter, pageable);
        List<LeaderboardDTO> dtos = users.getContent().stream()
                .map(DTOMapper.INSTANCE::convertToLeaderboardDTO)
                .toList();

        return new PageImpl<>(dtos, pageable, users.getTotalElements());
    }

    @PostMapping("/populate")
    public ResponseEntity<Void> populateLeaderboardIfEmpty() {
        if (userService.isUserTableEmpty()) {
            userService.populateUsersFromSQL();
        }
        return ResponseEntity.noContent().build();
    }
}
