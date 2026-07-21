package com.example.project.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.project.security.dto.AuthUserDTO;

@Controller
public class HomeController {

  @GetMapping({"", "/"})
  public String home(Model model, @AuthenticationPrincipal AuthUserDTO user) {
    model.addAttribute("loginUser", user);
    return "index";
  }

  @GetMapping("accessDenied")
  public void auth() {

  }

  @GetMapping("confirm")
  public String confirm() {
    return "/confirm";
  }

  @GetMapping("privacy")
  public String privacy() {
    return "/privacy";
  }

  @GetMapping("intro")
  public String introduce() {
    return "/introduction";
  }
}