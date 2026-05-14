package com.university.ojt.repository;

import com.university.ojt.model.OfficeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfficeSettingsRepository extends JpaRepository<OfficeSettings, Long> {
    Optional<OfficeSettings> findTopByOrderByIdAsc();
}
