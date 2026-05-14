package com.university.ojt.repository;

import com.university.ojt.model.AttendanceLog;
import com.university.ojt.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    List<AttendanceLog> findByStudentOrderByWorkDateDesc(Student student);

    Optional<AttendanceLog> findByStudentAndWorkDate(Student student, LocalDate workDate);

    boolean existsByStudentAndWorkDate(Student student, LocalDate workDate);

    List<AttendanceLog> findByStatus(AttendanceLog.AttendanceStatus status);

    @Query("SELECT a FROM AttendanceLog a WHERE a.student.id = :studentId ORDER BY a.workDate DESC")
    List<AttendanceLog> findByStudentIdOrderByWorkDateDesc(@Param("studentId") Long studentId);

    @Query("SELECT a FROM AttendanceLog a WHERE a.student.id = :studentId AND a.workDate BETWEEN :start AND :end ORDER BY a.workDate ASC")
    List<AttendanceLog> findByStudentIdAndDateRange(
            @Param("studentId") Long studentId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(a.hoursWorked), 0) FROM AttendanceLog a WHERE a.student.id = :studentId AND a.status IN ('APPROVED', 'APPROVED_LATE')")
    Double getTotalApprovedHours(@Param("studentId") Long studentId);

    @Query("SELECT a FROM AttendanceLog a WHERE a.status = 'PENDING' ORDER BY a.clockInTime ASC")
    List<AttendanceLog> findAllPending();

    @Query("SELECT COUNT(a) FROM AttendanceLog a WHERE a.student.id = :studentId AND a.clockOutTime IS NULL")
    long countOpenSessions(@Param("studentId") Long studentId);
}
