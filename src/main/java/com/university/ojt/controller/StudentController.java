package com.university.ojt.controller;

import com.university.ojt.model.AttendanceLog;
import com.university.ojt.model.DailyJournal;
import com.university.ojt.model.Student;
import com.university.ojt.model.OfficeSettings;
import com.university.ojt.repository.AttendanceLogRepository;
import com.university.ojt.repository.OfficeSettingsRepository;
import com.university.ojt.repository.StudentRepository;
import com.university.ojt.security.JwtUtils;
import com.university.ojt.service.AttendanceService;
import com.university.ojt.service.JournalService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('OJT_STUDENT')")
public class StudentController {

    @Autowired private AttendanceService attendanceService;
    @Autowired private JournalService journalService;
    @Autowired private StudentRepository studentRepo;
    @Autowired private AttendanceLogRepository attendanceRepo;
    @Autowired private OfficeSettingsRepository officeSettingsRepo;
    @Autowired private JwtUtils jwtUtils;

    private Long getStudentIdFromRequest(HttpServletRequest request) {
        String jwt = extractJwt(request);
        String email = jwtUtils.getEmailFromToken(jwt);
        return studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found")).getId();
    }

    private String extractJwt(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("jwt".equals(c.getName())) return c.getValue();
            }
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        throw new RuntimeException("No JWT found");
    }

    @PostMapping("/clock-in")
    public ResponseEntity<Map<String, Object>> clockIn(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        String photo = (String) body.get("photo");
        Double latitude = body.get("latitude") != null ? Double.parseDouble(body.get("latitude").toString()) : null;
        Double longitude = body.get("longitude") != null ? Double.parseDouble(body.get("longitude").toString()) : null;
        Double accuracy = body.get("accuracy") != null ? Double.parseDouble(body.get("accuracy").toString()) : null;
        
        return ResponseEntity.ok(attendanceService.clockIn(studentId, photo, latitude, longitude, accuracy));
    }

    @GetMapping("/office-location")
    public ResponseEntity<Map<String, Object>> getOfficeLocation() {
        OfficeSettings office = officeSettingsRepo.findTopByOrderByIdAsc().orElseGet(() -> {
            OfficeSettings defaultSettings = new OfficeSettings();
            return officeSettingsRepo.save(defaultSettings);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("latitude", office.getLatitude());
        response.put("longitude", office.getLongitude());
        response.put("radiusMeters", office.getRadiusMeters());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clock-out")
    public ResponseEntity<Map<String, Object>> clockOut(HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        return ResponseEntity.ok(attendanceService.clockOut(studentId));
    }

    @GetMapping("/attendance-status")
    public ResponseEntity<Map<String, Object>> getAttendanceStatus(HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        Map<String, Object> status = new HashMap<>();
        status.put("clockedIn", attendanceService.hasClockedInToday(studentId));
        status.put("clockedOut", attendanceService.hasClockedOutToday(studentId));
        status.put("incompleteJournal", journalService.getIncompleteJournal(studentId));
        return ResponseEntity.ok(status);
    }

    @GetMapping("/attendance-logs")
    public ResponseEntity<List<Map<String, Object>>> getAttendanceLogs(HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        List<AttendanceLog> logs = attendanceService.getStudentLogs(studentId);
        List<Map<String, Object>> result = logs.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("date", l.getWorkDate().toString());
            map.put("clockIn", l.getClockInTime() != null ? l.getClockInTime().toString() : null);
            map.put("clockOut", l.getClockOutTime() != null ? l.getClockOutTime().toString() : null);
            map.put("hours", l.getHoursWorked());
            map.put("status", l.getStatus().name());
            map.put("journalDone", l.isJournalCompleted());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        Student s = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", s.getId());
        profile.put("name", s.getFullName());
        profile.put("email", s.getEmail());
        profile.put("program", s.getProgram());
        profile.put("idNumber", s.getIdNumber());
        profile.put("yearLevel", s.getYearLevel());
        profile.put("section", s.getSection());
        profile.put("requiredHours", s.getRequiredHours());
        profile.put("completedHours", s.getCompletedHours());
        profile.put("progress", s.getProgressPercentage());
        profile.put("status", s.getStatus().name());
        profile.put("photo", s.getProfilePhotoPath());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/journal/save")
    public ResponseEntity<Map<String, Object>> saveJournal(
            @RequestParam Long attendanceId,
            @RequestParam String activities,
            @RequestParam String keyLearnings,
            @RequestParam String challenges,
            @RequestParam(required = false) MultipartFile popPhoto,
            HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        return ResponseEntity.ok(journalService.saveJournalEntry(
                studentId, attendanceId, activities, keyLearnings, challenges, popPhoto));
    }

    @GetMapping("/journals")
    public ResponseEntity<List<Map<String, Object>>> getJournals(HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        List<DailyJournal> journals = journalService.getStudentJournals(studentId);
        List<Map<String, Object>> result = journals.stream().map(j -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", j.getId());
            map.put("date", j.getEntryDate().toString());
            map.put("activities", j.getActivities());
            map.put("keyLearnings", j.getKeyLearnings());
            map.put("challenges", j.getChallenges());
            map.put("popPhoto", j.getPopPhotoPath());
            map.put("completed", j.isCompleted());
            map.put("attendanceId", j.getAttendance().getId());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/journal/allowed-days")
    public ResponseEntity<List<Map<String, Object>>> getAllowedJournalDays(HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        List<AttendanceLog> days = journalService.getAllowedJournalDays(studentId);
        List<Map<String, Object>> result = days.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("attendanceId", a.getId());
            map.put("date", a.getWorkDate().toString());
            map.put("journalDone", a.isJournalCompleted());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformance(HttpServletRequest request) {
        long studentId = getStudentIdFromRequest(request);
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        List<AttendanceLog> allLogs = attendanceRepo.findByStudentIdOrderByWorkDateDesc(studentId);


        LocalDate today = LocalDate.now();
        Map<String, Double> weeklyHours = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            double hrs = allLogs.stream()
                    .filter(l -> l.getWorkDate().equals(day)
                            && (l.getStatus() == AttendanceLog.AttendanceStatus.APPROVED
                                || l.getStatus() == AttendanceLog.AttendanceStatus.APPROVED_LATE)
                            && l.getHoursWorked() != null)
                    .mapToDouble(AttendanceLog::getHoursWorked).sum();
            weeklyHours.put(day.toString(), hrs);
        }


        Map<String, Double> monthlyHours = new LinkedHashMap<>();
        for (int w = 3; w >= 0; w--) {
            LocalDate weekStart = today.minusWeeks(w).with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
            LocalDate weekEnd = weekStart.plusDays(6);
            double hrs = allLogs.stream()
                    .filter(l -> !l.getWorkDate().isBefore(weekStart) && !l.getWorkDate().isAfter(weekEnd)
                            && (l.getStatus() == AttendanceLog.AttendanceStatus.APPROVED
                                || l.getStatus() == AttendanceLog.AttendanceStatus.APPROVED_LATE)
                            && l.getHoursWorked() != null)
                    .mapToDouble(AttendanceLog::getHoursWorked).sum();
            monthlyHours.put("Week " + (4 - w), hrs);
        }


        LocalDate weekStart = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        long thisWeekLates = allLogs.stream()
                .filter(l -> !l.getWorkDate().isBefore(weekStart)
                        && l.getStatus() == AttendanceLog.AttendanceStatus.APPROVED_LATE)
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("weeklyHours", weeklyHours);
        result.put("monthlyHours", monthlyHours);
        result.put("completedHours", student.getCompletedHours());
        result.put("requiredHours", student.getRequiredHours());
        result.put("progress", student.getProgressPercentage());
        result.put("thisWeekLates", thisWeekLates);
        result.put("status", student.getStatus().name());
        return ResponseEntity.ok(result);
    }
}
