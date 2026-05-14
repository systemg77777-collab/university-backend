package com.university.ojt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseUser {


    private String idNumber;

    @Column(name = "program_code")
    private String program;

    @Column(name = "year_level")
    private Integer yearLevel = 4;

    @Column(name = "section")
    @Enumerated(EnumType.STRING)
    private Section section;


    private String schoolName;

    private String phoneNumber;

    private String schoolIdFrontPath;
    private String schoolIdBackPath;

    @Column(nullable = false)
    private Double requiredHours;

    @Column(nullable = false)
    private Double completedHours = 0.0;

    @Column(nullable = false)
    private boolean fromSchool = true;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StudentStatus status = StudentStatus.ACTIVE;

    private java.time.LocalDate lastClockIn;


    @Override
    public String getDisplayIdentifier() {
        return idNumber != null ? idNumber : phoneNumber;
    }

    @Override
    public String getDashboardUrl() {
        return "/student/dashboard";
    }

    public double getProgressPercentage() {
        if (requiredHours == null || requiredHours == 0) return 0;
        return Math.min((completedHours / requiredHours) * 100, 100);
    }

    public enum Section {
        A, B, C, D
    }

    public enum StudentStatus {
        ACTIVE, INACTIVE, COMPLETED
    }
}
