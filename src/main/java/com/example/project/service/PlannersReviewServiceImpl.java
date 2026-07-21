package com.example.project.service;

import com.example.project.entity.Planners;
import com.example.project.entity.PlannersReview;
import com.example.project.entity.User;
import com.example.project.repository.PlannersRepository;
import com.example.project.repository.PlannersReviewRepository;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlannersReviewServiceImpl implements PlannersReviewService {

  private final PlannersReviewRepository plannersReviewRepository;
  private final PlannersRepository plannersRepository;
  private final UserRepository userRepository;

  // 리뷰 생성
  @Override
  public Long createReview(Long tid, Long uid, String title, Integer rating, String content){
    Planners planners = plannersRepository.findById(tid)
    .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

    User user = userRepository.findById(uid)
    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    if (hasReview(tid, uid)) {
      return 0L;
    }

    PlannersReview review = PlannersReview.builder()
      .planners(planners)
      .user(user)
      .title(title)
      .rating(rating)
      .content(content)
      .build();

    plannersReviewRepository.save(review);
    return 1L;
  }

  // 리뷰 수정
  @Override
  public void modifyReview(Long rid, Long uid, String reviewTitle, Integer rating, String content){
    PlannersReview review = plannersReviewRepository.findById(rid)
    .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
    if (!review.getUser().getUid().equals(uid)) {
    throw new IllegalArgumentException("본인이 작성한 리뷰만 수정할 수 있습니다.");
  }

    review.changeReview(reviewTitle, rating, content);
    plannersReviewRepository.save(review);
  }

  // 리뷰 삭제
  @Override
  public void deleteReview(Long rid, Long uid){
    PlannersReview review = plannersReviewRepository.findById(rid)
    .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
    if (!review.getUser().getUid().equals(uid)) {
    throw new IllegalArgumentException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
    }
    plannersReviewRepository.delete(review);
  }

  // 리뷰 목록 최신순으로 가져옴
  @Override
  public Page<PlannersReview> getReviewList(Long tid, Pageable pageable) {
    return plannersReviewRepository.findByPlanners_TidOrderByRegDateDesc(tid, pageable);
  }

  // 리뷰 가지고 있는지 확인
  @Override
  public boolean hasReview (Long tid, Long uid) {
    return plannersReviewRepository.existsByPlanners_TidAndUser_Uid(tid, uid);
  }

  // 리뷰 평균 점수 계산
  @Override
  public double getReviewAverage(Long tid) {
    List<PlannersReview> reviews = plannersReviewRepository.findByPlanners_Tid(tid);
    if (reviews.isEmpty()) {
      return 0.0;
    }
    double total = 0.0;
    for (PlannersReview review : reviews) {
      total += review.getRating();
    }
    return total / reviews.size();
  }
}