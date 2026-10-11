package com.smartnav.controller;

import com.smartnav.dto.ApiResponse;
import com.smartnav.dto.LeaderboardDto;
import com.smartnav.entity.User;
import com.smartnav.security.SecurityUtils;
import com.smartnav.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardRestController {

    private final LeaderboardService leaderboardService;
    private final SecurityUtils securityUtils;

    public LeaderboardRestController(LeaderboardService leaderboardService, SecurityUtils securityUtils) {
        this.leaderboardService = leaderboardService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaderboardDto>>> getLeaderboard() {
        List<LeaderboardDto> list = leaderboardService.getLeaderboard();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/my-rank")
    public ResponseEntity<ApiResponse<LeaderboardDto>> getMyRank() {
        User user = securityUtils.getCurrentUser().orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.ok("No user authenticated", null));
        }
        LeaderboardDto myRank = leaderboardService.getUserRank(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(myRank));
    }
}
