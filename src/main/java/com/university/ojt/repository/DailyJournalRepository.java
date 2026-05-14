package com.university.ojt.repository;

import com.university.ojt.model.DailyJournal;
import com.university.ojt.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyJournalRepository extends JpaRepository<DailyJournal, Long> {

    List<DailyJournal> findByStudentOrderByEntryDateDesc(Student student);

    Optional<DailyJournal> findByStudentAndEntryDate(Student student, LocalDate entryDate);

    Optional<DailyJournal> findByAttendanceId(Long attendanceId);

    @Query("SELECT j FROM DailyJournal j WHERE j.student.id = :studentId AND j.completed = false ORDER BY j.entryDate ASC")
    List<DailyJournal> findIncompleteJournals(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(j) FROM DailyJournal j WHERE j.student.id = :studentId AND j.completed = true")
    long countCompletedJournals(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(j) FROM DailyJournal j WHERE j.student.id = :studentId AND j.completed = false")
    long countIncompleteJournals(@Param("studentId") Long studentId);
}
