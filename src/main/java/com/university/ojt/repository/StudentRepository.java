package com.university.ojt.repository;

import com.university.ojt.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByIdNumber(String idNumber);

    boolean existsByEmail(String email);

    List<Student> findByStatus(Student.StudentStatus status);

    List<Student> findByFromSchool(boolean fromSchool);

    @Query("SELECT s FROM Student s WHERE s.fullName LIKE %:name% OR s.idNumber LIKE %:name%")
    List<Student> searchByNameOrId(@Param("name") String name);

    @Query("SELECT s FROM Student s WHERE s.lastClockIn <= :cutoffDate OR s.lastClockIn IS NULL")
    List<Student> findInactiveStudents(@Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT s FROM Student s ORDER BY s.fullName ASC")
    List<Student> findAllOrderedByName();
}
