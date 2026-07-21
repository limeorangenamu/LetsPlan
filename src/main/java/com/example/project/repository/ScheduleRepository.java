package com.example.project.repository;

import com.example.project.dto.PageRequestDTO;
import com.example.project.entity.PlannersUser;
import com.example.project.entity.ScheduleStatus;
import com.example.project.entity.Schedule;
import com.example.project.repository.search.SearchScheduleRepository;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long>, SearchScheduleRepository {
    // 플래너즈와 함께 조회
    @EntityGraph(attributePaths = { "planners" })
    Optional<Schedule> findById(Long sid);

    // 그룹아이디로 스케쥴 찾기 쿼리
    @EntityGraph(attributePaths = { "planners" })
    Page<Schedule> findByPlanners_Tid(Long tid, Pageable pageable);

    // 그룹아이디 및 상태를 미리 가져와서 필터링
    @EntityGraph(attributePaths = { "planners" })
    Page<Schedule> findByPlanners_TidAndScheduleStatusNot(Long tid, ScheduleStatus scheduleStatus, Pageable pageable);

    @EntityGraph(attributePaths = { "planners" })
    Page<Schedule> findByPlanners_TidAndScheduleStatus(Long tid, ScheduleStatus scheduleStatus, Pageable pageable);

    // 유저아이디와 플래너즈 아이디로 내가 만든, 참가한 일정이 존재하는지 여부 조회
    @Query("select case when count(sp.schedule) > 0 then true else false end from ScheduleParticipant sp where sp.user.uid = :uid and sp.schedule.planners.tid = :tid and sp.schedule.scheduleStatus <> 'ENDED'")
    boolean existsMySchedule(@Param("tid") Long tid, @Param("uid") Long uid);

    // 유저아이디와 플래너즈 아이디로 내가 만든, 참가한 일정 목록 조회 (페이징 X)
    @EntityGraph(attributePaths = { "planners" })
    @Query("select sp.schedule from ScheduleParticipant sp where sp.user.uid = :uid and sp.schedule.planners.tid = :tid and sp.schedule.scheduleStatus <> 'ENDED'")
    List<Schedule> findByPlanners_TidAndUser_UidWithNoFilterNoPage(@Param("tid") Long tid, @Param("uid") Long uid);

    //and sp.schedule.scheduleStatus <> 'ENDED'
    // 유저아이디와 플래너즈 아이디로 내가 만든, 참가한 일정 목록 조회 이거 쓰시면 돼요 - 20260626 관언 (하드코딩)
    @EntityGraph(attributePaths = { "planners" })
    @Query("select sp.schedule from ScheduleParticipant sp where sp.user.uid = :uid and sp.schedule.planners.tid = :tid order by sp.schedule.regDate desc")
    Page<Schedule> findByPlanners_TidAndUser_UidWithNoFilter(@Param("tid") Long tid, @Param("uid") Long uid, Pageable pageable);

    // 유저아이디와 플래너즈 아이디로 내가 만든, 참가한 일정 목록 조회 (필터 적용)
    @EntityGraph(attributePaths = { "planners" })
    @Query("select sp.schedule from ScheduleParticipant sp where sp.user.uid = :uid and sp.schedule.planners.tid = :tid and sp.schedule.scheduleStatus = :filter order by sp.schedule.regDate desc")
    Page<Schedule> findByPlanners_TidAndUser_UidWithFilter(@Param("tid") Long tid, @Param("uid") Long uid, @Param("filter") ScheduleStatus filter, Pageable pageable);

    // 엔디드로 수정
    @Transactional
    @Modifying
    @Query("""
            UPDATE Schedule s
            SET s.scheduleStatus = 'ENDED'
            WHERE s.endDate < :now
            AND s.scheduleStatus <> 'ENDED'
            """)
    int updateEndedSchedules(@Param("now") LocalDateTime now);

    boolean existsByTitle(String title);

    boolean existsBySidAndCreator_Uid(Long sid, Long uid);

    // 해당 일정이 마감됐는지 여부 확인
    @Query("select count(s) > 0 from Schedule s where s.sid = :sid and s.scheduleStatus = 'CLOSED'")
    boolean isClosed(@Param("sid") Long sid);

    @Query("select s from Schedule s where DATE(s.startDate) = :targetDate and s.scheduleStatus = 'CLOSED'")
    List<Schedule> findSchedulesByDate(@Param("targetDate") LocalDate targetDate);
}