package com.university.ojt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "office_settings")
@Getter
@Setter
@NoArgsConstructor
public class OfficeSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String officeName = "Colegio de Montalban";

    @Column(nullable = false)
    private Double latitude = 14.7504;

    @Column(nullable = false)
    private Double longitude = 121.1417;

    @Column(nullable = false)
    private Integer radiusMeters = 100;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
