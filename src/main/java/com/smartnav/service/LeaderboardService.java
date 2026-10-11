package com.smartnav.service;

import com.smartnav.dto.LeaderboardDto;
import com.smartnav.entity.LeaderboardEntry;
import com.smartnav.entity.User;
import com.smartnav.repository.LeaderboardRepository;
import com.smartnav.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final UserRepository userRepository;

    public LeaderboardService(LeaderboardRepository leaderboardRepository, UserRepository userRepository) {
        this.leaderboardRepository = leaderboardRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordReportSubmission(User user) {
        user.addPoints(2); // +2 points for reporting
        userRepository.save(user);
        syncUserToLeaderboard(user, 1, 0);
    }

    @Transactional
    public void recordReportApproval(User user) {
        user.addPoints(10); // +10 points for approved verification
        userRepository.save(user);
        syncUserToLeaderboard(user, 0, 1);
    }

    @Transactional
    public void syncUserToLeaderboard(User user, int submittedInc, int approvedInc) {
        Optional<LeaderboardEntry> existing = leaderboardRepository.findByUserId(user.getId());
        LeaderboardEntry entry = existing.orElseGet(() -> {
            LeaderboardEntry newEntry = new LeaderboardEntry();
            newEntry.setUser(user);
            newEntry.setReportsSubmitted(0);
            newEntry.setReportsApproved(0);
            return newEntry;
        });

        entry.setPoints(user.getPoints());
        entry.setBadge(user.getBadge());
        entry.setReportsSubmitted(entry.getReportsSubmitted() + submittedInc);
        entry.setReportsApproved(entry.getReportsApproved() + approvedInc);
        leaderboardRepository.save(entry);

        recalculateRanks();
    }

    @Transactional
    public void recalculateRanks() {
        List<LeaderboardEntry> all = leaderboardRepository.findAllByOrderByPointsDesc();
        int rank = 1;
        for (LeaderboardEntry entry : all) {
            entry.setRankPosition(rank++);
            leaderboardRepository.save(entry);
        }
    }

    public List<LeaderboardDto> getLeaderboard() {
        List<LeaderboardEntry> entries = leaderboardRepository.findAllByOrderByPointsDesc();
        List<LeaderboardDto> dtoList = new ArrayList<>();
        int fallbackRank = 1;
        for (LeaderboardEntry entry : entries) {
            int rank = entry.getRankPosition() != null && entry.getRankPosition() > 0 ? entry.getRankPosition() : fallbackRank++;
            dtoList.add(new LeaderboardDto(
                    entry.getUser().getId(),
                    entry.getUser().getName(),
                    entry.getPoints(),
                    entry.getBadge(),
                    rank,
                    entry.getReportsSubmitted(),
                    entry.getReportsApproved()
            ));
        }
        return dtoList;
    }

    public LeaderboardDto getUserRank(Long userId) {
        return leaderboardRepository.findByUserId(userId)
                .map(entry -> new LeaderboardDto(
                        entry.getUser().getId(),
                        entry.getUser().getName(),
                        entry.getPoints(),
                        entry.getBadge(),
                        entry.getRankPosition() != null ? entry.getRankPosition() : 1,
                        entry.getReportsSubmitted(),
                        entry.getReportsApproved()
                ))
                .orElse(null);
    }
}
