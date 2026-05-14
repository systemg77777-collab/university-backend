-- ============================================================
-- UNI-Versity OJT System Database Schema
-- MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS university_ojt
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE university_ojt;

-- ─── STUDENTS ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS students (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name           VARCHAR(150)    NOT NULL,
    email               VARCHAR(200)    UNIQUE NOT NULL,
    password            VARCHAR(255)    NOT NULL,
    role                VARCHAR(30)     NOT NULL DEFAULT 'OJT_STUDENT',
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    approved            BOOLEAN         NOT NULL DEFAULT TRUE,
    from_school         BOOLEAN         NOT NULL DEFAULT TRUE,
    id_number           VARCHAR(50),
    program_code        VARCHAR(20),
    year_level          INT             DEFAULT 4,
    section             ENUM('A','B','C','D'),
    school_name         VARCHAR(200),
    phone_number        VARCHAR(20),
    school_id_front_path VARCHAR(500),
    school_id_back_path  VARCHAR(500),
    profile_photo_path  VARCHAR(500),
    required_hours      DOUBLE          NOT NULL DEFAULT 500,
    completed_hours     DOUBLE          NOT NULL DEFAULT 0,
    status              ENUM('ACTIVE','INACTIVE','COMPLETED') NOT NULL DEFAULT 'ACTIVE',
    last_clock_in       DATE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ─── ADMINS ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admins (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(150)    NOT NULL,
    email       VARCHAR(200)    UNIQUE NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(30)     NOT NULL DEFAULT 'ADMIN',
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    approved    BOOLEAN         NOT NULL DEFAULT FALSE,
    id_number   VARCHAR(50)     NOT NULL,
    institute   VARCHAR(100)    NOT NULL,
    profile_photo_path VARCHAR(500),
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ─── SUPERADMINS ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS superadmins (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(150)    NOT NULL,
    email       VARCHAR(200)    UNIQUE NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(30)     NOT NULL DEFAULT 'SUPERADMIN',
    enabled     BOOLEAN         NOT NULL DEFAULT FALSE,
    approved    BOOLEAN         NOT NULL DEFAULT FALSE,
    id_number   VARCHAR(50)     NOT NULL,
    position    VARCHAR(150)    NOT NULL,
    institute   VARCHAR(200)    NOT NULL,
    profile_photo_path VARCHAR(500),
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ─── ADMIN REQUESTS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(150)    NOT NULL,
    email           VARCHAR(200)    UNIQUE NOT NULL,
    id_number       VARCHAR(50)     NOT NULL,
    institute       VARCHAR(100)    NOT NULL,
    hashed_password VARCHAR(255)    NOT NULL,
    status          ENUM('PENDING','APPROVED','DENIED') NOT NULL DEFAULT 'PENDING',
    requested_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     DATETIME,
    reviewed_by     BIGINT
) ENGINE=InnoDB;

-- ─── ATTENDANCE LOGS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS attendance_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id          BIGINT          NOT NULL,
    work_date           DATE            NOT NULL,
    clock_in_time       DATETIME        NOT NULL,
    clock_out_time      DATETIME,
    clock_in_photo_path VARCHAR(500),
    clock_in_lat        DOUBLE,          -- geolocation audit: student latitude at clock-in
    clock_in_lng        DOUBLE,          -- geolocation audit: student longitude at clock-in
    hours_worked        DOUBLE,
    status              ENUM('PENDING','APPROVED','APPROVED_LATE','DENIED') NOT NULL DEFAULT 'PENDING',
    journal_completed   BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    UNIQUE KEY uk_student_workdate (student_id, work_date)
) ENGINE=InnoDB;

-- ─── DAILY JOURNALS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS daily_journals (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT          NOT NULL,
    attendance_id   BIGINT          NOT NULL UNIQUE,
    entry_date      DATE            NOT NULL,
    activities      TEXT,
    key_learnings   TEXT,
    challenges      TEXT,
    pop_photo_path  VARCHAR(500),
    completed       BOOLEAN         NOT NULL DEFAULT FALSE,
    submitted_at    DATETIME,
    CONSTRAINT fk_journal_student    FOREIGN KEY (student_id)   REFERENCES students(id)        ON DELETE CASCADE,
    CONSTRAINT fk_journal_attendance FOREIGN KEY (attendance_id) REFERENCES attendance_logs(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── OFFICE SETTINGS (Geofence) ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS office_settings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    office_name     VARCHAR(200)    NOT NULL DEFAULT 'Colegio de Montalban',
    latitude        DOUBLE          NOT NULL DEFAULT 14.7504,
    longitude       DOUBLE          NOT NULL DEFAULT 121.1417,
    radius_meters   INT             NOT NULL DEFAULT 100,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Seed default office location (Colegio de Montalban, Rodriguez, Rizal)
INSERT IGNORE INTO office_settings (id, office_name, latitude, longitude, radius_meters)
VALUES (1, 'Colegio de Montalban, Rodriguez, Rizal', 14.7504, 121.1417, 100);

-- ─── INDEXES ──────────────────────────────────────────────────────────────────
CREATE INDEX idx_attendance_student  ON attendance_logs(student_id);
CREATE INDEX idx_attendance_date     ON attendance_logs(work_date);
CREATE INDEX idx_attendance_status   ON attendance_logs(status);
CREATE INDEX idx_journal_student     ON daily_journals(student_id);
CREATE INDEX idx_student_status      ON students(status);
CREATE INDEX idx_student_last_clock  ON students(last_clock_in);
