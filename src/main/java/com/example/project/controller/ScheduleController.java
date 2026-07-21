package com.example.project.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.project.dto.PageRequestDTO;
import com.example.project.dto.PageResultDTO;
import com.example.project.dto.RegisterScheduleDTO;
import com.example.project.dto.ResponsePlannersDTO;
import com.example.project.dto.ResponseScheduleDTO;
import com.example.project.dto.ScheduleBlockDTO;
import com.example.project.entity.Schedule;
import com.example.project.entity.ScheduleStatus;
import com.example.project.security.dto.AuthUserDTO;
import com.example.project.service.FavoriteService;
import com.example.project.service.PlannersService;
import com.example.project.service.ScheduleService;
import com.example.project.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/planners/schedule")
public class ScheduleController {

  private final String prefix = "/planners/schedule/";

  @Value("${com.example.upload.path}")
  private String uploadPath;

  private final PlannersService plannersService;
  private final ScheduleService scheduleService;
  private final FavoriteService favoriteService;
  private final UserService userService;

  private void getPlannersDTO(Long tid, Model model, @AuthenticationPrincipal AuthUserDTO user) {
    // 플래너즈 정보
    ResponsePlannersDTO responsePlannersDTO = plannersService.getPlannersById(tid);
    // 로그인 유저 식별
    Long uid = user != null ? user.getUid() : null;
    if (responsePlannersDTO != null) {
      model.addAttribute("plannersModifyThumbnail", responsePlannersDTO.getPlannersThumbnail());
      model.addAttribute("plannersModifyBanner", responsePlannersDTO.getPlannersBanner());

      if (responsePlannersDTO.getPlannersBanner().equals("/img/banner.png"))
        responsePlannersDTO.setPlannersBanner(
            "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1800&q=80");
      if (responsePlannersDTO.getPlannersThumbnail().equals("/img/thumbnail.png"))
        responsePlannersDTO.setPlannersThumbnail("/img/empty_thumbnail.png");
      model.addAttribute("plannersDTO", responsePlannersDTO);
      model.addAttribute("currentUserUid", uid);
      model.addAttribute("owner", plannersService.getOwner(tid));
      model.addAttribute("isMember", plannersService.isMember(tid, uid));
      model.addAttribute("tid", tid);
      // model.addAttribute("memberList",plannersService.getPlannersUserByTid(tid));
      // 식별한 유저를 통해 찜하기 여부 제출
      if (uid != null && favoriteService.get(uid).contains(tid))
        model.addAttribute("active", "active");
    }
  }

  // 나의 일정 목록 페이지(참가자,생성자 모두) 감사합니다
  @GetMapping("calendar")
  public String mylist(@AuthenticationPrincipal AuthUserDTO user,
                       @RequestParam(value = "filter", required = false) String status,
                       @RequestParam("tid") Long tid, Model model, @RequestParam(value = "scpage", defaultValue = "1") int scpage,
                       PageRequestDTO pageRequestDTO) {

    getPlannersDTO(tid, model, user);

    Long uid = (user != null) ? user.getUid() : null; // null-safe 처리
    PageResultDTO<ResponseScheduleDTO, Schedule> result = scheduleService.getUserScheduleList(tid, uid, scpage, status);

    model.addAttribute("scheduleList", result);
    return prefix + "calendar";
  }

  // @GetMapping("calendar")
  // public String mylist(@AuthenticationPrincipal AuthUserDTO user,
  // @RequestParam(value = "filter", required = false) String status,
  // @RequestParam("tid") Long tid, Model model, @RequestParam(value = "scpage",
  // defaultValue = "1") int scpage
  // ,PageRequestDTO pageRequestDTO) {

  // getPlannersDTO(tid, model, user);

  // Long uid = (user != null) ? user.getUid() : null; // null-safe 처리
  // PageResultDTO<ResponseScheduleDTO, Schedule> result =
  // scheduleService.getUserScheduleList(tid, uid, scpage, status);

  // ObjectMapper mapper = new ObjectMapper();
  // mapper.registerModule(new JavaTimeModule());

  // model.addAttribute(
  // "scheduleJson",
  // mapper.writeValueAsString(
  // result.getDtoList()
  // )
  // );
  // return prefix + "mylist";
  // }

  // 일정 목록 페이지
  @GetMapping({ "", "/", "board" })
  public String list(@AuthenticationPrincipal AuthUserDTO user,
                     @RequestParam(value = "filter", required = false) String status,
                     @RequestParam("tid") Long tid, Model model, @RequestParam(value = "scpage", defaultValue = "1") int scpage,
                     PageRequestDTO pageRequestDTO) {

    getPlannersDTO(tid, model, user);

    Long uid = (user != null) ? user.getUid() : null; // null-safe 처리
    pageRequestDTO.setPage(scpage);
    PageResultDTO<ResponseScheduleDTO, Schedule> result = scheduleService.getScheduleList(tid, uid, pageRequestDTO,
        status);

    model.addAttribute("scheduleList", result);

    return prefix + "board";
  }

  // 일정 정보 불러오기
  @GetMapping("/view")
  public ResponseEntity<Map<String, Object>> getSchedule(@AuthenticationPrincipal AuthUserDTO user,
                                                         @RequestParam("tid") Long tid, @RequestParam("sid") Long sid) {
    // uid가 null일 수 있으니 안전하게 처리
    Long uid = (user != null) ? user.getUid() : null;
    Map<String, Object> body = new HashMap<>();
    ResponseScheduleDTO result = scheduleService.get(sid, uid);
    if (result != null) {
      body.put("schDTO", result);
      body.put("isMember", plannersService.isMember(tid, uid));
      return ResponseEntity.ok(body);
    }
    return ResponseEntity.notFound().build();
  }

  // 일정 등록 작성 페이지
  @GetMapping("invite")
  public String createPlan(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("tid") Long tid, Model model,
                           PageRequestDTO pageRequestDTO) {
    // 플래너즈 DTO
    getPlannersDTO(tid, model, user);
    return prefix + "invite";
  }

  // 일정 등록
  @PostMapping("invite")
  public String submitPlan(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("tid") Long tid, Model model,
                           @RequestParam(value = "scheduleThumbnailFile", required = false) MultipartFile scheduleThumbnail,
                           RegisterScheduleDTO registerScheduleDTO, PageRequestDTO pageRequestDTO) {

    try {
      scheduleService.createSchedule(tid, user.getUid(), registerScheduleDTO, scheduleThumbnail, uploadPath);

    } catch (IllegalArgumentException | IOException e) {

      return "redirect:/planners/schedule/invite?createError="
          + URLEncoder.encode(
          e.getMessage(),
          StandardCharsets.UTF_8);
    }

    return "redirect:/planners/schedule?tid=" + tid;
  }

  // 일정 작업 페이지
  @GetMapping("editor")
  public String editon(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("tid") Long tid, Model model,
                       PageRequestDTO pageRequestDTO, @RequestParam("sid") Long sid) {

    // PageResultDTO<ResponseScheduleDTO, Schedule> result =
    // scheduleService.getScheduleList(tid, pageRequestDTO); findyBySid

    getPlannersDTO(tid, model, user);
    // 스케쥴 정보 넘김
    Long uid = (user != null) ? user.getUid() : null;
    if (uid == null) {
      return "redirect:/planners/schedule?tid=" + tid;
    }
    ResponseScheduleDTO schedule = scheduleService.get(sid, uid);
    if (schedule.getStatus().equals(ScheduleStatus.SCHEDULED)) {
      return "redirect:/planners/schedule/detail?tid=" + tid + "&sid=" + sid;
    }
    model.addAttribute("user", userService.searchUserByUid(uid));
    model.addAttribute("schedule", schedule);
    // 주인장인지 보내주기
    model.addAttribute("isCreator", scheduleService.isCreator(sid, uid));
    // 여기에 scheduleRepo에서 sid로 find한 모델전송
    model.addAttribute("sid", sid);
    model.addAttribute("tid", tid);
    return prefix + "editor";
  }

  // 조정 완료된 일정 상세보기 페이지
  @GetMapping("detail")
  public String detail(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("tid") Long tid, Model model,
                       PageRequestDTO pageRequestDTO, @RequestParam("sid") Long sid) {

    // PageResultDTO<ResponseScheduleDTO, Schedule> result =
    // scheduleService.getScheduleList(tid, pageRequestDTO); findyBySid
    // 기본
    getPlannersDTO(tid, model, user);
    // 스케쥴 정보 넘김
    Long uid = (user != null) ? user.getUid() : null;
    if (uid == null) {
      return "redirect:/planners/schedule?tid=" + tid;
    }
    ResponseScheduleDTO schedule = scheduleService.get(sid, uid);
    if (schedule.getStatus().equals(ScheduleStatus.PENDING)) {
      return "redirect:/planners/schedule/editor?tid=" + tid + "&sid=" + sid;
    }
    model.addAttribute("schedule", schedule);
    model.addAttribute("participants", scheduleService.getScheduleParticipantsList(sid));
    model.addAttribute("blocks", scheduleService.getScheduleBlockList(sid));
    // 주인장인지 보내주기
    model.addAttribute("isCreator", scheduleService.isCreator(sid, uid));
    model.addAttribute("isParticipant", scheduleService.isParticipant(uid, sid));
    model.addAttribute("isClosed", scheduleService.isClosed(sid));
    // 여기에 scheduleRepo에서 sid로 find한 모델전송
    model.addAttribute("sid", sid);
    model.addAttribute("tid", tid);

    return prefix + "detail";
  }

  // 일정 작업 등록
  @PostMapping("/submit")
  @ResponseBody
  public ResponseEntity<Void> submitSchedule(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("sid") Long sid,
                                             @RequestBody List<ScheduleBlockDTO> scheduleBlockDTO) {
    Long result = scheduleService.submitSchedule(sid, scheduleBlockDTO);
    if (result == 1L) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else if (result == -1L) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // 일정 참가하기
  @PostMapping("/participate")
  @ResponseBody
  public ResponseEntity<Void> participate(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("sid") Long sid) {
    Long uid = (user != null) ? user.getUid() : null;
    Long result = scheduleService.participate(uid, sid);
    if (result == 1L) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else if (result == -1L) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } else if (result == -2L) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // 일정 참가 취소
  @PostMapping("/cancel")
  @ResponseBody
  public ResponseEntity<Void> cancelParticipate(@AuthenticationPrincipal AuthUserDTO user,
                                                @RequestParam("sid") Long sid) {
    Long uid = (user != null) ? user.getUid() : null;
    Long result = scheduleService.cancelParticipate(uid, sid);
    if (result == 1L) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else if (result == -1L) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // 일정 이름 변경
  @PostMapping("/title")
  @ResponseBody
  public ResponseEntity<Void> modifyTitle(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("sid") Long sid,
                                          @RequestParam("title") String title) {
    Long uid = (user != null) ? user.getUid() : null;
    if (scheduleService.isCreator(sid, uid)) {
      Long result = scheduleService.modifyScheduleTitle(sid, title);
      if (result == 1L) {
        return new ResponseEntity<>(HttpStatus.OK);
      } else if (result == 0L) {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
      }
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // 일정 지우기
  @DeleteMapping("")
  @ResponseBody
  public ResponseEntity<Void> removeSchedule(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("sid") Long sid) {
    Long uid = (user != null) ? user.getUid() : null;
    Long result = scheduleService.removeSchedule(sid, uid);
    if (result == 1L) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else if (result == 0L) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // 모집 마감
  @PostMapping("/close")
  @ResponseBody
  public ResponseEntity<Void> closeSchedule(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("sid") Long sid) {
    Long uid = (user != null) ? user.getUid() : null;
    Long result = scheduleService.closeSchedule(sid, uid);
    if (result == 1L) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else if (result == 0L) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // 내 일정이 존재하는지 여부 조회
  @GetMapping("/mylist")
  @ResponseBody
  public boolean existsMySchedule(@AuthenticationPrincipal AuthUserDTO user, @RequestParam("tid") Long tid) {
    Long uid = (user != null) ? user.getUid() : null;
    return scheduleService.existsMySchedule(tid, uid);
  }
}