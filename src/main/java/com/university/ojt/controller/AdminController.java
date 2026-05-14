package com.university.ojt.controller;

import com.university.ojt.model.*;
import com.university.ojt.repository.*;
import com.university.ojt.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
public class AdminController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private AttendanceLogRepository attendanceRepo;
    @Autowired private AdminRequestRepository adminRequestRepo;
    @Autowired private AdminRepository adminRepo;
    @Autowired private AttendanceService attendanceService;




    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        List<Student> all = studentRepo.findAll();
        long total = all.size();
        long activeToday = all.stream()
                .filter(s -> s.getLastClockIn() != null && s.getLastClockIn().equals(LocalDate.now()))
                .count();
        long pending = adminRequestRepo.countByStatus(AdminRequest.RequestStatus.PENDING);
        long pendingAttendance = attendanceRepo.findAllPending().size();

        Map<String, Object> result = new HashMap<>();
        result.put("totalInterns", total);
        result.put("activeToday", total > 0 ? (activeToday * 100.0 / total) : 0);
        result.put("pendingAdminRequests", pending);
        result.put("pendingAttendance", pendingAttendance);
        return ResponseEntity.ok(result);
    }



    @GetMapping("/students")
    public ResponseEntity<List<Map<String, Object>>> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page) {
        List<Student> students;
        if (search != null && !search.isBlank()) {
            students = studentRepo.searchByNameOrId(search);
        } else if (status != null && !status.isBlank()) {
            students = studentRepo.findByStatus(Student.StudentStatus.valueOf(status));
        } else {
            students = studentRepo.findAllOrderedByName();
        }

        int start = page * 10;
        int end = Math.min(start + 10, students.size());
        List<Student> paged = start < students.size() ? students.subList(start, end) : List.of();

        List<Map<String, Object>> result = paged.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getFullName());
            map.put("photo", s.getProfilePhotoPath());
            map.put("program", s.getProgram());
            map.put("idNumber", s.getIdNumber());
            map.put("status", s.getStatus().name());
            map.put("isOnline", attendanceRepo.countOpenSessions(s.getId()) > 0);
            map.put("completedHours", s.getCompletedHours());
            map.put("requiredHours", s.getRequiredHours());
            map.put("progress", s.getProgressPercentage());
            map.put("startDate", s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate().toString() : null);
            map.put("email", s.getEmail());
            map.put("phone", s.getPhoneNumber());
            map.put("school", s.isFromSchool() ? "UNI-Versity" : s.getSchoolName());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/students/total")
    public ResponseEntity<Map<String, Object>> getStudentCounts() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", studentRepo.count());
        result.put("active", studentRepo.findByStatus(Student.StudentStatus.ACTIVE).size());
        result.put("inactive", studentRepo.findByStatus(Student.StudentStatus.INACTIVE).size());
        result.put("completed", studentRepo.findByStatus(Student.StudentStatus.COMPLETED).size());
        return ResponseEntity.ok(result);
    }



    @GetMapping("/attendance/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingAttendance() {
        List<AttendanceLog> pending = attendanceService.getPendingAttendance();
        List<Map<String, Object>> result = pending.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("studentId", l.getStudent().getId());
            map.put("studentName", l.getStudent().getFullName());
            map.put("studentPhoto", l.getStudent().getProfilePhotoPath());
            map.put("clockInPhoto", l.getClockInPhotoPath());
            map.put("clockInTime", l.getClockInTime().toString());
            map.put("clockOutTime", l.getClockOutTime() != null ? l.getClockOutTime().toString() : null);
            map.put("workDate", l.getWorkDate().toString());
            map.put("hours", l.getHoursWorked());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/attendance/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveAttendance(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean isLate) {
        return ResponseEntity.ok(attendanceService.reviewAttendance(id, true, isLate));
    }

    @PostMapping("/attendance/{id}/deny")
    public ResponseEntity<Map<String, Object>> denyAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.reviewAttendance(id, false, false));
    }



    @GetMapping("/admin-requests")
    public ResponseEntity<List<Map<String, Object>>> getAdminRequests() {
        List<AdminRequest> requests = adminRequestRepo.findByStatus(AdminRequest.RequestStatus.PENDING);
        List<Map<String, Object>> result = requests.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("name", r.getFullName());
            map.put("email", r.getEmail());
            map.put("idNumber", r.getIdNumber());
            map.put("institute", r.getInstitute());
            map.put("requestedAt", r.getRequestedAt().toString());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin-requests/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveAdminRequest(@PathVariable long id) {
        AdminRequest req = adminRequestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        Admin admin = new Admin();
        admin.setFullName(req.getFullName());
        admin.setEmail(req.getEmail());
        admin.setPassword(req.getHashedPassword());
        admin.setIdNumber(req.getIdNumber());
        admin.setInstitute(Admin.Institute.valueOf(req.getInstitute()));
        admin.setRole(BaseUser.UserRole.ADMIN);
        admin.setEnabled(true);
        admin.setApproved(true);
        adminRepo.save(admin);
        req.setStatus(AdminRequest.RequestStatus.APPROVED);
        adminRequestRepo.save(req);
        return ResponseEntity.ok(Map.of("success", true, "message", "Admin approved!"));
    }

    @PostMapping("/admin-requests/{id}/deny")
    public ResponseEntity<Map<String, Object>> denyAdminRequest(@PathVariable long id) {
        AdminRequest req = adminRequestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus(AdminRequest.RequestStatus.DENIED);
        adminRequestRepo.save(req);
        return ResponseEntity.ok(Map.of("success", true, "message", "Request denied."));
    }



    @GetMapping("/students/{id}/logs")
    public ResponseEntity<List<Map<String, Object>>> getStudentLogs(@PathVariable Long id) {
        List<AttendanceLog> logs = attendanceRepo.findByStudentIdOrderByWorkDateDesc(id);
        List<Map<String, Object>> result = logs.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("date", l.getWorkDate().toString());
            map.put("clockIn", l.getClockInTime() != null ? l.getClockInTime().toString() : null);
            map.put("clockOut", l.getClockOutTime() != null ? l.getClockOutTime().toString() : null);
            map.put("hours", l.getHoursWorked());
            map.put("status", l.getStatus().name());
            map.put("photo", l.getClockInPhotoPath());
            map.put("journalDone", l.isJournalCompleted());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
