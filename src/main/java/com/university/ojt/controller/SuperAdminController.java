package com.university.ojt.controller;

import com.university.ojt.model.*;
import com.university.ojt.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private AttendanceLogRepository attendanceRepo;
    @Autowired private SuperAdminRepository superAdminRepo;
    @Autowired private OfficeSettingsRepository officeSettingsRepo;



    @GetMapping("/confidential/students")
    public ResponseEntity<List<Map<String, Object>>> getConfidentialStudents(
            @RequestParam(required = false) String search) {
        List<Student> students = search != null && !search.isBlank()
                ? studentRepo.searchByNameOrId(search)
                : studentRepo.findAllOrderedByName();

        List<Map<String, Object>> result = students.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getFullName());
            map.put("email", s.getEmail());
            map.put("idNumber", s.getIdNumber());
            map.put("program", s.getProgram());
            map.put("yearLevel", s.getYearLevel());
            map.put("section", s.getSection());
            map.put("requiredHours", s.getRequiredHours());
            map.put("completedHours", s.getCompletedHours());
            map.put("status", s.getStatus().name());
            map.put("phone", s.getPhoneNumber());
            map.put("school", s.isFromSchool() ? "UNI-Versity" : s.getSchoolName());
            map.put("fromSchool", s.isFromSchool());
            map.put("startDate", s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate().toString() : null);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/confidential/students/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, Object>> updateStudentRecord(
            @PathVariable long id,
            @RequestBody Map<String, Object> updates) {
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (updates.containsKey("fullName")) student.setFullName((String) updates.get("fullName"));
        if (updates.containsKey("email")) student.setEmail((String) updates.get("email"));
        if (updates.containsKey("idNumber")) student.setIdNumber((String) updates.get("idNumber"));
        if (updates.containsKey("program")) student.setProgram((String) updates.get("program"));
        if (updates.containsKey("yearLevel"))
            student.setYearLevel(((Number) updates.get("yearLevel")).intValue());
        if (updates.containsKey("section"))
            student.setSection(Student.Section.valueOf((String) updates.get("section")));
        if (updates.containsKey("requiredHours"))
            student.setRequiredHours(((Number) updates.get("requiredHours")).doubleValue());
        if (updates.containsKey("phone")) student.setPhoneNumber((String) updates.get("phone"));

        studentRepo.save(student);
        return ResponseEntity.ok(Map.of("success", true, "message", "Student record updated!"));
    }



    @GetMapping("/reports/overview")
    public ResponseEntity<Map<String, Object>> getOverallReports() {
        List<Student> all = studentRepo.findAll();
        if (all.isEmpty()) return ResponseEntity.ok(Map.of("error", "No students found."));

        long total = all.size();
        long completed = all.stream().filter(s -> s.getStatus() == Student.StudentStatus.COMPLETED).count();
        long active = all.stream().filter(s -> s.getStatus() == Student.StudentStatus.ACTIVE).count();
        long inactive = all.stream().filter(s -> s.getStatus() == Student.StudentStatus.INACTIVE).count();


        LocalDate weekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        long compliant = all.stream()
                .filter(s -> s.getLastClockIn() != null && !s.getLastClockIn().isBefore(weekStart))
                .count();

        double complianceRate = total > 0 ? (compliant * 100.0 / total) : 0;


        double avgProgress = all.stream().mapToDouble(Student::getProgressPercentage).average().orElse(0);


        Map<String, Double> weeklyByStudent = new LinkedHashMap<>();
        for (Student s : all) {
            double hrs = attendanceRepo.findByStudentIdAndDateRange(s.getId(), weekStart, LocalDate.now())
                    .stream()
                    .filter(l -> (l.getStatus() == AttendanceLog.AttendanceStatus.APPROVED
                                  || l.getStatus() == AttendanceLog.AttendanceStatus.APPROVED_LATE)
                            && l.getHoursWorked() != null)
                    .mapToDouble(AttendanceLog::getHoursWorked).sum();
            weeklyByStudent.put(s.getFullName(), hrs);
        }


        String conclusion;
        if (complianceRate >= 80) {
            conclusion = String.format("%.0f%% of OJT trainees are actively following through! Keep it up, everyone! 🎉", complianceRate);
        } else if (complianceRate >= 50) {
            conclusion = String.format("%.0f%% compliance this week. Some trainees need a gentle reminder — let's go! 💪", complianceRate);
        } else {
            conclusion = String.format("Only %.0f%% are complying this week. Time for a serious check-in! ⚠️", complianceRate);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("completed", completed);
        result.put("active", active);
        result.put("inactive", inactive);
        result.put("complianceRate", Math.round(complianceRate));
        result.put("avgProgress", Math.round(avgProgress));
        result.put("weeklyHoursByStudent", weeklyByStudent);
        result.put("conclusion", conclusion);
        return ResponseEntity.ok(result);
    }



    @GetMapping("/pending-superadmins")
    public ResponseEntity<List<Map<String, Object>>> getPendingSuperAdmins() {
        List<SuperAdmin> pending = superAdminRepo.findAll().stream()
                .filter(sa -> !sa.isApproved()).collect(Collectors.toList());
        List<Map<String, Object>> result = pending.stream().map(sa -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sa.getId());
            map.put("name", sa.getFullName());
            map.put("email", sa.getEmail());
            map.put("position", sa.getPosition());
            map.put("institute", sa.getInstitute());
            map.put("reason", sa.getReason());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/pending-superadmins/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveSuperAdmin(@PathVariable long id) {
        SuperAdmin sa = superAdminRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("SuperAdmin not found"));
        sa.setApproved(true);
        sa.setEnabled(true);
        superAdminRepo.save(sa);
        return ResponseEntity.ok(Map.of("success", true));
    }



    @GetMapping("/office-settings")
    public ResponseEntity<OfficeSettings> getOfficeSettings() {
        OfficeSettings office = officeSettingsRepo.findTopByOrderByIdAsc().orElseGet(() -> {
            OfficeSettings defaultSettings = new OfficeSettings();
            return officeSettingsRepo.save(defaultSettings);
        });
        return ResponseEntity.ok(office);
    }

    @PutMapping("/office-settings")
    public ResponseEntity<Map<String, Object>> updateOfficeSettings(@RequestBody Map<String, Object> body) {
        OfficeSettings office = officeSettingsRepo.findTopByOrderByIdAsc().orElseGet(() -> new OfficeSettings());
        
        if (body.containsKey("officeName")) office.setOfficeName((String) body.get("officeName"));
        if (body.containsKey("latitude")) office.setLatitude(Double.parseDouble(body.get("latitude").toString()));
        if (body.containsKey("longitude")) office.setLongitude(Double.parseDouble(body.get("longitude").toString()));
        if (body.containsKey("radiusMeters")) office.setRadiusMeters(Integer.parseInt(body.get("radiusMeters").toString()));
        
        officeSettingsRepo.save(office);
        return ResponseEntity.ok(Map.of("success", true, "message", "Office location settings updated successfully!"));
    }
}
