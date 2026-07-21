package com.example.project.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.project.dto.RegisterUserDTO;
import com.example.project.dto.ResponseUserDTO;
import com.example.project.security.dto.AuthUserDTO;
import com.example.project.security.service.CustomUserDetailsService;
import com.example.project.service.NotificationService;
import com.example.project.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

  @Value("${com.example.upload.path}")
  private String uploadPath;

  private final UserService userService;
  private final NotificationService notificationService;
  private final CustomUserDetailsService customUserDetailsService;

  // 로그인 페이지
  @GetMapping("/login")
  public String login(@RequestParam(value = "error", required = false) String error, Model model) {
    if (error != null) {
      model.addAttribute("errorMessage", "이메일 또는 비밀번호를 확인해주세요.");
    }
    return "user/login";
  }

  // 회원가입 페이지
  @GetMapping("/register")
  public String register() {
    return "user/register";
  }

  // 회원가입
  @PostMapping("/register")
  public String register(RegisterUserDTO registerUserDTO, @RequestParam(value = "error", required = false) String error,
                         @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
    try {
      userService.registerUser(registerUserDTO, profileImage, uploadPath);
      return "redirect:/user/login";
    } catch (IllegalArgumentException | IOException e) {
      return "redirect:/user/register?registerError=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
    }
  }

  // 로그아웃
  @GetMapping("/logout")
  public String logout() {
    return "user/logout";
  }

  // 마이페이지
  @GetMapping("/mypage")
  public String myPage(@AuthenticationPrincipal AuthUserDTO user, Model model) {
    if (user != null) {
      Long uid = user != null ? user.getUid() : null;
      model.addAttribute("user", user);
      model.addAttribute("notificationsCount", notificationService.getNotificationsCount(uid));
    }

    return "user/mypage";
  }

  // 회원정보 수정 페이지
  @GetMapping("/modify")
  public String modifyForm(@AuthenticationPrincipal AuthUserDTO user, Model model) {
    if (user != null) {
      model.addAttribute("user", user);
    }

    return "user/modify";
  }

  // 회원정보 수정
  @PostMapping("/modify")
  public String modify(RegisterUserDTO registerUserDTO, @RequestParam(value = "profileFile", required = false)
                       MultipartFile profileFile, @AuthenticationPrincipal AuthUserDTO user,
                       @RequestParam("removeFile") boolean removeFile, RedirectAttributes redirectAttributes) {
    try {
      ResponseUserDTO updatedUser = userService.modifyUser(registerUserDTO, profileFile, uploadPath, removeFile);

      // 정보 수정 후 Authentication 갱신
      UserDetails userDetails = customUserDetailsService.loadUserByUsername(updatedUser.getEmail());
      Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
      Authentication newAuth = new UsernamePasswordAuthenticationToken(userDetails, currentAuth.getCredentials(), userDetails.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(newAuth);

      return "redirect:/user/mypage";

    } catch (DataIntegrityViolationException e) {
      redirectAttributes.addFlashAttribute("modifyError", "이미 존재하는 이름입니다.");
      return "redirect:/user/modify";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("modifyError", e.getMessage());
      return "redirect:/user/modify";
    }
  }
}