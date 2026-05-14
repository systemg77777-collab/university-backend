package com.university.ojt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "attendance_logs")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private LocalDateTime clockInTime;

    private LocalDateTime clockOutTime;

    private String clockInPhotoPath;

    private Double clockInLat;
    
    private Double clockInLng;

    private Double hoursWorked;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status = AttendanceStatus.PENDING;

    private boolean journalCompleted = false;


    public boolean isValidClockOut() {
        if (clockOutTime == null || clockInTime == null) return false;
        long minutes = java.time.Duration.between(clockInTime, clockOutTime).toMinutes();
        return minutes >= 30;
    }

    public void computeHours() {
        if (clockInTime != null && clockOutTime != null) {
            hoursWorked = java.time.Duration.between(clockInTime, clockOutTime).toMinutes() / 60.0;
        }
    }

    public enum AttendanceStatus {
        PENDING, APPROVED, APPROVED_LATE, DENIED
    }
}
