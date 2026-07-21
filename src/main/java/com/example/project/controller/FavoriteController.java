package com.example.project.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.security.dto.AuthUserDTO;
import com.example.project.service.FavoriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/favorite")
public class FavoriteController {
  private final FavoriteService favoriteService;

  @PostMapping
  public ResponseEntity<Long> add(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("tid") Long tid) {
    Long uid = user != null? user.getUid() : null;
    if (uid == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);  // 로그인 필요
    }
    Long result = favoriteService.add(uid, tid);
    if (result == 1L) {
      return new ResponseEntity<>(result, HttpStatus.CREATED);  // 찜하기 등록
    } else if (result == 0L) {
      return new ResponseEntity<>(result, HttpStatus.OK);   // 찜하기 삭제
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @GetMapping("/")
  public ResponseEntity<List<Long>> get(@RequestParam("uid") Long uid) {
    List<Long> result = favoriteService.get(uid);
    if (!result.isEmpty()) {
      return new ResponseEntity<>(result, HttpStatus.OK);
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @DeleteMapping("/")
  public ResponseEntity<Void> remove(@RequestParam("uid") Long uid, @RequestParam("tid") Long tid) {
    try {
      favoriteService.remove(uid, tid);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
