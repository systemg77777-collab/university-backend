package com.university.ojt.controller;

import com.university.ojt.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {

        Map<String, Object> result = authService.login(body.get("email"), body.get("password"));

        if (result.containsKey("token")) {

            Cookie cookie = new Cookie("jwt", (String) result.get("token"));
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(86400);
            response.addCookie(cookie);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("success", true, "redirect", "/"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.forgotPassword(body.get("email")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.resetPassword(body.get("token"), body.get("newPassword")));
    }

    @PostMapping("/register/student")
    public ResponseEntity<Map<String, Object>> registerStudent(
            @RequestParam Map<String, String> data,
            @RequestParam(required = false) MultipartFile idFront,
            @RequestParam(required = false) MultipartFile idBack,
            @RequestParam(required = false) MultipartFile profilePhoto) {
        return ResponseEntity.ok(authService.registerStudent(data, idFront, idBack, profilePhoto));
    }

    @PostMapping("/register/admin-request")
    public ResponseEntity<Map<String, Object>> submitAdminRequest(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.submitAdminRequest(body));
    }

    @PostMapping("/register/superadmin-apply")
    public ResponseEntity<Map<String, Object>> applySuperAdmin(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.submitSuperAdminApplication(body));
    }
}
