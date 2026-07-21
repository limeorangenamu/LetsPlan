package com.example.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.dto.GuestbookDTO;
import com.example.project.dto.PageRequestDTO;
import com.example.project.dto.PageResultDTO;
import com.example.project.entity.Guestbook;
import com.example.project.security.dto.AuthUserDTO;
import com.example.project.service.GuestbookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/planners/guestbook")
@Log4j2
@RequiredArgsConstructor
public class GuestbookRestController {

  private final GuestbookService guestbookService;

  @GetMapping("/{tid}")
  public ResponseEntity<PageResultDTO<GuestbookDTO, Guestbook>> getList(@PathVariable("tid") Long tid,
                                                                        @ModelAttribute PageRequestDTO pageRequestDTO) {

    // 강제로 방명록은 페이지당 5개씩 가져오도록 설정
    pageRequestDTO.setSize(5);

    PageResultDTO<GuestbookDTO, Guestbook> result = guestbookService.getList(tid, pageRequestDTO);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @PostMapping("/{tid}")
  public ResponseEntity<Long> register(@PathVariable("tid") Long tid, @RequestBody GuestbookDTO guestbookDTO,
                                       @AuthenticationPrincipal AuthUserDTO authMember) {

    if (authMember == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    guestbookDTO.setTid(tid);
    guestbookDTO.setWriterUid(authMember.getUid());

    Long gid = guestbookService.register(guestbookDTO);

    return new ResponseEntity<>(gid, HttpStatus.OK);
  }

  @PutMapping("/{gid}")
  public ResponseEntity<String> modify(@PathVariable("gid") Long gid, @RequestBody GuestbookDTO dto) {
    dto.setGid(gid);
    guestbookService.modify(dto);
    return new ResponseEntity<>("success", HttpStatus.OK);
  }

  @DeleteMapping("/{gid}")
  public ResponseEntity<String> remove(@PathVariable("gid") Long gid) {
    // 삭제 실행 (보안을 더 강화하려면 여기서 현재 사용자(authMember)가
    // 작성자 본인이거나 해당 플래너의 관리자(owner)인지 DB에서 확인하는 로직을 추가하시면 더 좋습니다.)
    guestbookService.remove(gid);
    return new ResponseEntity<>("success", HttpStatus.OK);
  }
}