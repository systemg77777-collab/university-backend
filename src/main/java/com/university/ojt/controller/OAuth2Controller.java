package com.university.ojt.controller;

import com.university.ojt.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import com.university.ojt.repository.StudentRepository;
import com.university.ojt.repository.AdminRepository;
import com.university.ojt.repository.SuperAdminRepository;
import java.io.IOException;


@Controller
public class OAuth2Controller {

    @Autowired private JwtUtils jwtUtils;
    @Autowired private StudentRepository studentRepo;
    @Autowired private AdminRepository adminRepo;
    @Autowired private SuperAdminRepository superAdminRepo;

    @GetMapping("/oauth2/success")
    public void oauth2Success(@AuthenticationPrincipal OAuth2User principal,
                              HttpServletResponse response) throws IOException {
        String email = principal.getAttribute("email");
        String role = "OJT_STUDENT";
        String redirect = "/";

        if (superAdminRepo.findByEmail(email).isPresent()) {
            role = "SUPERADMIN";
            redirect = "/superadmin/dashboard.html";
        } else if (adminRepo.findByEmail(email).isPresent()) {
            role = "ADMIN";
            redirect = "/admin/dashboard.html";
        } else if (studentRepo.findByEmail(email).isPresent()) {
            role = "OJT_STUDENT";
            redirect = "/student/dashboard.html";
        } else {

            response.sendRedirect("/?googleEmail=" + email + "&googleName="
                    + principal.getAttribute("name"));
            return;
        }

        String token = jwtUtils.generateJwtToken(email, role);
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        response.addCookie(cookie);
        response.sendRedirect(redirect);
    }
}
