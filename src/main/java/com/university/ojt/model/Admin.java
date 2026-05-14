package com.university.ojt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends BaseUser {

    @Column(nullable = false)
    private String idNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Institute institute;

    @Override
    public String getDisplayIdentifier() {
        return idNumber;
    }

    @Override
    public String getDashboardUrl() {
        return "/admin/dashboard";
    }

    public enum Institute {
        INSTITUTE_OF_COMPUTING_STUDIES("Institute of Computing Studies"),
        INSTITUTE_OF_TEACHER_EDUCATION("Institute of Teacher Education"),
        INSTITUTE_OF_BUSINESS_ENTREPRENEURSHIP("Institute of Business Entrepreneurship");

        private final String displayName;

        Institute(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
