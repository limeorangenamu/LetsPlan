package com.example.project.repository;

import com.example.project.entity.PlannersReview;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface PlannersReviewRepository extends JpaRepository<PlannersReview, Long> {
  
  // 플래너즈 id로 리뷰 목록을 regDate 최신순으로 조회하는 메서드
  @EntityGraph(attributePaths = {"user"})
  Page<PlannersReview> findByPlanners_TidOrderByRegDateDesc(Long tid, Pageable pageable);  

  // 플래너즈 id로 리뷰 목록 조회 (정렬 X)
  List<PlannersReview> findByPlanners_Tid(Long tid);

  // 해당 사용자가 해당 플래너즈에 리뷰를 작성하였는지 여부 조회
  boolean existsByPlanners_TidAndUser_Uid(Long tid, Long uid);
}
