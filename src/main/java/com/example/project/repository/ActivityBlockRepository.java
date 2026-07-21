package com.example.project.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import static com.example.project.entity.BlockStatus.REMOVED;

import com.example.project.entity.ActivityBlock;

public interface ActivityBlockRepository extends JpaRepository<ActivityBlock, Long> {
  // 플래너즈 id로 활동 블럭 조회 (REMOVED가 아닌 것만)
  @Query("select a from ActivityBlock a where a.planners.tid = :tid and a.status <> REMOVED")
  List<ActivityBlock> getActivityBlockList(@Param("tid") Long tid);

  @Modifying
  @Transactional
  @Query("update ActivityBlock a set a.status = REMOVED where a.id = :bid")
  void removeBlock(@Param("bid") Long bid);
}
