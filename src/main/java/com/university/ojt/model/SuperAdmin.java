package com.university.ojt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "superadmins")
@Getter
@Setter
@NoArgsConstructor
public class SuperAdmin extends BaseUser {

    @Column(nullable = false)
    private String idNumber;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private String institute;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Override
    public String getDisplayIdentifier() {
        return idNumber;
    }

    @Override
    public String getDashboardUrl() {
        return "/superadmin/dashboard";
    }
}
