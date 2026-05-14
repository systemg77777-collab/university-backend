package com.university.ojt.config;

import com.university.ojt.model.Admin;
import com.university.ojt.model.BaseUser;
import com.university.ojt.model.SuperAdmin;
import com.university.ojt.repository.AdminRepository;
import com.university.ojt.repository.SuperAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired private AdminRepository adminRepository;
    @Autowired private SuperAdminRepository superAdminRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedDefaultAdmin();
        seedDefaultSuperAdmin();
    }

    private void seedDefaultAdmin() {
        if (!adminRepository.existsByEmail("ADMIN001@university.edu")) {
            Admin admin = new Admin();
            admin.setFullName("Default Administrator");
            admin.setEmail("ADMIN001@university.edu");
            admin.setPassword(passwordEncoder.encode("12341234"));
            admin.setIdNumber("ADMIN001");
            admin.setInstitute(Admin.Institute.INSTITUTE_OF_COMPUTING_STUDIES);
            admin.setRole(BaseUser.UserRole.ADMIN);
            admin.setEnabled(true);
            admin.setApproved(true);
            adminRepository.save(admin);
            logger.info("✅ Default Admin seeded: ADMIN001 / 12341234");
        }
    }

    private void seedDefaultSuperAdmin() {
        if (!superAdminRepository.existsByEmail("superadmin@university.edu")) {
            SuperAdmin sa = new SuperAdmin();
            sa.setFullName("System Super Administrator");
            sa.setEmail("superadmin@university.edu");
            sa.setPassword(passwordEncoder.encode("docjoy"));
            sa.setIdNumber("SA001");
            sa.setPosition("President");
            sa.setInstitute("Institute of Computing Studies");
            sa.setRole(BaseUser.UserRole.SUPERADMIN);
            sa.setEnabled(true);
            sa.setApproved(true);
            superAdminRepository.save(sa);
            logger.info("✅ Default SuperAdmin seeded: superadmin / docjoy");
        }
    }
}
