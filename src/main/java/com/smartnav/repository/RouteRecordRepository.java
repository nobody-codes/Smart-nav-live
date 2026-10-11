package com.smartnav.repository;

import com.smartnav.entity.RouteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRecordRepository extends JpaRepository<RouteRecord, Long> {

    List<RouteRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RouteRecord> findAllByOrderByCreatedAtDesc();
}
