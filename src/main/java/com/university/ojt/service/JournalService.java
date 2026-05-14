package com.university.ojt.service;

import com.university.ojt.model.AttendanceLog;
import com.university.ojt.model.DailyJournal;
import com.university.ojt.model.Student;
import com.university.ojt.repository.AttendanceLogRepository;
import com.university.ojt.repository.DailyJournalRepository;
import com.university.ojt.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class JournalService {

    @Autowired private DailyJournalRepository journalRepo;
    @Autowired private AttendanceLogRepository attendanceRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private FileStorageService fileStorage;

    @Transactional
    public Map<String, Object> saveJournalEntry(long studentId, long attendanceId,
                                                  String activities, String keyLearnings,
                                                  String challenges, MultipartFile popPhoto) {
        Map<String, Object> result = new HashMap<>();
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        AttendanceLog attendance = attendanceRepo.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));

        if (!attendance.getStudent().getId().equals(studentId)) {
            result.put("error", "Unauthorized."); return result;
        }

        Optional<DailyJournal> existing = journalRepo.findByAttendanceId(attendanceId);
        DailyJournal journal = existing.orElseGet(DailyJournal::new);
        journal.setStudent(student);
        journal.setAttendance(attendance);
        journal.setEntryDate(attendance.getWorkDate());
        journal.setActivities(activities);
        journal.setKeyLearnings(keyLearnings);
        journal.setChallenges(challenges);

        if (popPhoto != null && !popPhoto.isEmpty()) {
            journal.setPopPhotoPath(fileStorage.saveFile(popPhoto, "photos"));
        }

        journalRepo.save(journal);


        if (journal.isCompleted()) {
            attendance.setJournalCompleted(true);
            attendanceRepo.save(attendance);
        }

        result.put("success", true);
        result.put("completed", journal.isCompleted());
        result.put("message", journal.isCompleted()
                ? "Journal entry saved and completed! ✅"
                : "Journal saved. Please fill all sections to complete.");
        return result;
    }

    public List<DailyJournal> getStudentJournals(long studentId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return journalRepo.findByStudentOrderByEntryDateDesc(student);
    }

    public Map<String, Object> getIncompleteJournal(long studentId) {
        List<DailyJournal> incomplete = journalRepo.findIncompleteJournals(studentId);
        Map<String, Object> result = new HashMap<>();
        if (!incomplete.isEmpty()) {
            DailyJournal j = incomplete.get(0);
            result.put("hasIncomplete", true);
            result.put("journalId", j.getId());
            result.put("entryDate", j.getEntryDate().toString());
            result.put("attendanceId", j.getAttendance().getId());
        } else {
            result.put("hasIncomplete", false);
        }
        return result;
    }

    public Optional<DailyJournal> getJournalByAttendance(long attendanceId) {
        return journalRepo.findByAttendanceId(attendanceId);
    }


    public List<AttendanceLog> getAllowedJournalDays(long studentId) {
        return attendanceRepo.findByStudentIdOrderByWorkDateDesc(studentId).stream()
                .filter(a -> a.getStatus() != AttendanceLog.AttendanceStatus.DENIED
                          && a.getClockOutTime() != null)
                .toList();
    }
}
