package com.university.ojt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "daily_journals")
@Getter
@Setter
@NoArgsConstructor
public class DailyJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private AttendanceLog attendance;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(columnDefinition = "TEXT")
    private String activities;

    @Column(columnDefinition = "TEXT")
    private String keyLearnings;

    @Column(columnDefinition = "TEXT")
    private String challenges;

    private String popPhotoPath;

    @Column(nullable = false)
    private boolean completed = false;

    private LocalDateTime submittedAt;

    @PrePersist
    @PreUpdate
    protected void checkCompletion() {
        completed = activities != null && !activities.isBlank()
                && keyLearnings != null && !keyLearnings.isBlank()
                && challenges != null && !challenges.isBlank()
                && popPhotoPath != null && !popPhotoPath.isBlank();
        if (completed && submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
