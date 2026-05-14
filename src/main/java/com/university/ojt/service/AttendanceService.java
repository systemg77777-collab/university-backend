package com.university.ojt.service;

import com.university.ojt.model.AttendanceLog;

import com.university.ojt.model.OfficeSettings;
import com.university.ojt.model.Student;
import com.university.ojt.repository.AttendanceLogRepository;
import com.university.ojt.repository.DailyJournalRepository;
import com.university.ojt.repository.OfficeSettingsRepository;
import com.university.ojt.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class AttendanceService {

    @Autowired private AttendanceLogRepository attendanceRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private DailyJournalRepository journalRepo;
    @Autowired private OfficeSettingsRepository officeSettingsRepo;
    @Autowired private FileStorageService fileStorage;
    

    private OfficeSettings getOfficeSettings() {
        return officeSettingsRepo.findTopByOrderByIdAsc()
                .orElseGet(() -> {
                    OfficeSettings defaultSettings = new OfficeSettings();
                    return officeSettingsRepo.save(defaultSettings);
                });
    }




    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }



    @Transactional
    public Map<String, Object> clockIn(long studentId, String base64Photo, Double latitude, Double longitude, Double accuracy) {
        Map<String, Object> result = new HashMap<>();
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        LocalDate today = LocalDate.now();


        if (attendanceRepo.existsByStudentAndWorkDate(student, today)) {
            result.put("error", "You have already clocked in today. You can only clock in once per day.");
            return result;
        }


        Optional<AttendanceLog> lastLog = attendanceRepo
                .findByStudentIdOrderByWorkDateDesc(studentId).stream().findFirst();
        if (lastLog.isPresent()
                && lastLog.get().getClockOutTime() != null
                && !lastLog.get().isJournalCompleted()
                && lastLog.get().getStatus() != AttendanceLog.AttendanceStatus.DENIED) {
            result.put("error", "Please complete your Daily Journal from "
                    + lastLog.get().getWorkDate() + " before clocking in again!");
            result.put("journalPending", true);
            result.put("pendingDate", lastLog.get().getWorkDate().toString());
            return result;
        }


        if (latitude == null || longitude == null) {
            result.put("error", "Location is required to clock in. Please enable GPS and allow location access in your browser.");
            return result;
        }

        if (accuracy != null && accuracy > 10000) {
            result.put("error", "Your GPS signal is too weak (accuracy: " + Math.round(accuracy) + "m). Please try again or move to an area with better signal.");
            return result;
        }

        OfficeSettings office = getOfficeSettings();
        double distance = calculateDistance(latitude, longitude, office.getLatitude(), office.getLongitude());
        
        if (distance > office.getRadiusMeters()) {
            result.put("error", "You are too far from the office. You are " + Math.round(distance) + " meters away, but must be within " + office.getRadiusMeters() + " meters.");
            return result;
        }


        String photoPath = null;
        if (base64Photo != null && !base64Photo.isEmpty()) {
            photoPath = fileStorage.saveBase64Photo(base64Photo, "photos",
                    "clockin_" + studentId + "_" + today);
        }

        AttendanceLog log = new AttendanceLog();
        log.setStudent(student);
        log.setWorkDate(today);
        log.setClockInTime(LocalDateTime.now());
        log.setClockInPhotoPath(photoPath);
        log.setClockInLat(latitude);
        log.setClockInLng(longitude);
        log.setStatus(AttendanceLog.AttendanceStatus.PENDING);
        attendanceRepo.save(log);

        student.setLastClockIn(today);
        student.setStatus(Student.StudentStatus.ACTIVE);
        studentRepo.save(student);

        result.put("success", true);
        result.put("attendanceId", log.getId());
        result.put("clockInTime", log.getClockInTime().toString());
        result.put("message", "Clock-in recorded! Work hard today! 💪");
        return result;
    }



    @Transactional
    public Map<String, Object> clockOut(long studentId) {
        Map<String, Object> result = new HashMap<>();
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        LocalDate today = LocalDate.now();
        Optional<AttendanceLog> logOpt = attendanceRepo.findByStudentAndWorkDate(student, today);

        if (logOpt.isEmpty() || logOpt.get().getClockInTime() == null) {
            result.put("error", "No active clock-in found for today."); return result;
        }

        AttendanceLog log = logOpt.get();
        if (log.getClockOutTime() != null) {
            result.put("error", "You already clocked out today."); return result;
        }

        log.setClockOutTime(LocalDateTime.now());


        if (!log.isValidClockOut()) {
            log.setClockOutTime(null);
            attendanceRepo.save(log);
            result.put("error", "Invalid clock out! Work at least 30 minutes per day!");
            result.put("invalidClockOut", true);
            return result;
        }

        log.computeHours();
        attendanceRepo.save(log);


        if (log.getHoursWorked() != null) {
            student.setCompletedHours(student.getCompletedHours() + log.getHoursWorked());
            checkCompletion(student);
            studentRepo.save(student);
        }

        result.put("success", true);
        result.put("hoursWorked", String.format("%.2f", log.getHoursWorked()));
        result.put("message", "Clock-out recorded! Great work today! 🎉");
        result.put("attendanceId", log.getId());
        return result;
    }



    @Transactional
    public Map<String, Object> reviewAttendance(long attendanceId, boolean approve, boolean isLate) {
        Map<String, Object> result = new HashMap<>();
        AttendanceLog log = attendanceRepo.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));

        if (approve) {
            log.setStatus(isLate ? AttendanceLog.AttendanceStatus.APPROVED_LATE
                    : AttendanceLog.AttendanceStatus.APPROVED);

            result.put("message", "Attendance approved!");
        } else {
            log.setStatus(AttendanceLog.AttendanceStatus.DENIED);

            if (log.getHoursWorked() != null) {
                Student student = log.getStudent();
                student.setCompletedHours(Math.max(0, student.getCompletedHours() - log.getHoursWorked()));
                if (student.getStatus() == Student.StudentStatus.COMPLETED && student.getCompletedHours() < student.getRequiredHours()) {
                    student.setStatus(Student.StudentStatus.ACTIVE);
                }
                studentRepo.save(student);
            }
            result.put("message", "Attendance denied.");
        }

        attendanceRepo.save(log);
        result.put("success", true);
        return result;
    }

    private void checkCompletion(Student student) {
        if (student.getCompletedHours() >= student.getRequiredHours()) {
            long incompleteJournals = journalRepo.countIncompleteJournals(student.getId());
            if (incompleteJournals == 0) {
                student.setStatus(Student.StudentStatus.COMPLETED);
            }
        }
    }



    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markInactiveStudents() {
        LocalDate cutoff = LocalDate.now().minusDays(4);
        List<Student> inactives = studentRepo.findInactiveStudents(cutoff);
        for (Student s : inactives) {
            if (s.getStatus() == Student.StudentStatus.ACTIVE) {
                s.setStatus(Student.StudentStatus.INACTIVE);
                studentRepo.save(s);
            }
        }
    }



    public List<AttendanceLog> getStudentLogs(long studentId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRepo.findByStudentOrderByWorkDateDesc(student);
    }

    public List<AttendanceLog> getPendingAttendance() {
        return attendanceRepo.findAllPending();
    }

    public boolean hasClockedInToday(long studentId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRepo.existsByStudentAndWorkDate(student, LocalDate.now());
    }

    public boolean hasClockedOutToday(long studentId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Optional<AttendanceLog> log = attendanceRepo.findByStudentAndWorkDate(student, LocalDate.now());
        return log.isPresent() && log.get().getClockOutTime() != null;
    }
}
