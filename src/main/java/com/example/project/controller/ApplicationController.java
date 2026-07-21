package com.example.project.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.dto.ApplicationDTO;
import com.example.project.security.dto.AuthUserDTO;
import com.example.project.service.ApplicationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/planners/join")
@RequiredArgsConstructor
public class ApplicationController {


  private final ApplicationService applicationService;

  // 가입 양식 불러오기
  @GetMapping("/{tid}/form")
  public ResponseEntity<?> getForm(@PathVariable("tid") Long tid) {

    String jsonSchema = applicationService.getForm(tid);
    return ResponseEntity.ok(jsonSchema);
  }

  // 가입 양식 저장
  @PostMapping("/{tid}/form")
  public ResponseEntity<?> saveForm(@PathVariable("tid") Long tid, @RequestBody ApplicationDTO.FormSaveRequest request)
      throws Exception {
    applicationService.saveForm(tid, request);
    return ResponseEntity.ok().build();
  }

  // 가입 신청
  @PostMapping("/{tid}/apply")
  public ResponseEntity<?> submitApplication(@PathVariable("tid") Long tid, @RequestBody ApplicationDTO.SubmitRequest request,
                                             @AuthenticationPrincipal AuthUserDTO authUser) throws Exception {
    Long result = applicationService.submitApplication(tid, request, authUser.getUid());
    if (result == 1L) {
      return new ResponseEntity<>(HttpStatus.OK);   // Response status 200 반환
    } else if (result == -1L) {
      return new ResponseEntity<>(HttpStatus.CONFLICT); // Response status 409 반환
    }
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);  // Response status 400 반환
  }

  // 가입 신청 목록 불러오기
  @GetMapping("/{tid}/applications")
  public ResponseEntity<List<ApplicationDTO.ApplicationResponse>> getApplications(@PathVariable("tid") Long tid) {
    List<ApplicationDTO.ApplicationResponse> responses = applicationService.getApplications(tid);
    return ResponseEntity.ok(responses);
  }

  // 가입 신청 승인
  @PostMapping("/applications/{applicationId}/approve")
  public ResponseEntity<?> approveApplication(@PathVariable("applicationId") Long applicationId) {
    Long result = applicationService.approveApplication(applicationId);
    if (result == 1L) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else if (result == -1L) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  // 가입 신청 거부
  @PostMapping("/applications/{applicationId}/reject")
  public ResponseEntity<?> rejectApplication(@PathVariable("applicationId") Long applicationId) {
    applicationService.rejectApplication(applicationId);
    return ResponseEntity.ok().build();
  }
}