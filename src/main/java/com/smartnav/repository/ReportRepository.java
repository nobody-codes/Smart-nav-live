package com.smartnav.repository;

import com.smartnav.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByOrderByCreatedAtDesc();

    List<Report> findByStatusOrderByCreatedAtDesc(String status);

    List<Report> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(String status);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT r.category, COUNT(r) FROM Report r GROUP BY r.category")
    List<Object[]> countReportsByCategory();

    @Query("SELECT r FROM Report r WHERE " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:userId IS NULL OR r.user.id = :userId) AND " +
           "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR r.createdAt <= :endDate) " +
           "ORDER BY r.createdAt DESC")
    List<Report> filterReports(@Param("category") String category,
                               @Param("status") String status,
                               @Param("userId") Long userId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    // Fetch approved blocking reports (Road Block, Accident, Construction, Flood, Pothole, Vehicle Breakdown, Fire)
    @Query("SELECT r FROM Report r WHERE r.status = 'APPROVED' AND r.category IN ('ROAD_BLOCK', 'ACCIDENT', 'CONSTRUCTION', 'FLOOD', 'POTHOLE', 'VEHICLE_BREAKDOWN', 'FIRE')")
    List<Report> findApprovedObstacles();

    // Fetch approved congestion reports (Traffic Jam)
    @Query("SELECT r FROM Report r WHERE r.status = 'APPROVED' AND r.category = 'TRAFFIC_JAM'")
    List<Report> findApprovedCongestions();
}
