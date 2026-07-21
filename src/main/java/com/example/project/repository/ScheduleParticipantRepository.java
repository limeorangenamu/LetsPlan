package com.example.project.repository;

import com.example.project.entity.ScheduleParticipant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
  boolean existsBySchedule_SidAndUser_Uid(Long sid, Long uid);

  List<ScheduleParticipant> findBySchedule_Sid(Long sid);

  Optional<ScheduleParticipant> findBySchedule_SidAndUser_Uid(Long sid, Long uid);

  // 플래너즈에서 탈퇴 시 해당 플래너즈에서 참가하고 있는 일정 목록 조회
  @EntityGraph(attributePaths = {"schedule", "user", "schedule.planners"})
  @Query("select sp from ScheduleParticipant sp where sp.user.uid = :uid and sp.schedule.planners.tid = :tid")
  List<ScheduleParticipant> findByUser_UidAndPlanners_Tid(@Param("uid") Long uid, @Param("tid") Long tid);
}
