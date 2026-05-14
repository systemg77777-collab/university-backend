package com.university.ojt.service;

import com.university.ojt.model.*;
import com.university.ojt.repository.*;
import com.university.ojt.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {

    @Autowired private StudentRepository studentRepo;
    @Autowired private AdminRepository adminRepo;
    @Autowired private SuperAdminRepository superAdminRepo;
    @Autowired private AdminRequestRepository adminRequestRepo;
    @Autowired private PasswordResetTokenRepository resetTokenRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private FileStorageService fileStorage;
    @Autowired private EmailService emailService;



    public Map<String, Object> login(String emailOrId, String password) {
        Map<String, Object> result = new HashMap<>();


        Optional<SuperAdmin> sa = superAdminRepo.findByEmail(
                emailOrId.equalsIgnoreCase("superadmin") ? "superadmin@university.edu" : emailOrId);
        if (sa.isPresent() && passwordEncoder.matches(password, sa.get().getPassword())) {
            if (!sa.get().isApproved()) { result.put("error", "Account pending approval."); return result; }
            result.put("token", jwtUtils.generateJwtToken(sa.get().getEmail(), "SUPERADMIN"));
            result.put("role", "SUPERADMIN");
            result.put("name", sa.get().getFullName());
            result.put("redirect", "/superadmin/dashboard.html");
            return result;
        }


        Optional<Admin> admin = adminRepo.findByEmail(emailOrId);
        if (admin.isEmpty()) {

            admin = adminRepo.findAll().stream()
                    .filter(a -> a.getIdNumber().equalsIgnoreCase(emailOrId)).findFirst();
        }
        if (admin.isPresent() && passwordEncoder.matches(password, admin.get().getPassword())) {
            if (!admin.get().isApproved()) { result.put("error", "Account pending approval from superadmin."); return result; }
            result.put("token", jwtUtils.generateJwtToken(admin.get().getEmail(), "ADMIN"));
            result.put("role", "ADMIN");
            result.put("name", admin.get().getFullName());
            result.put("redirect", "/admin/dashboard.html");
            return result;
        }


        Optional<Student> student = studentRepo.findByEmail(emailOrId);
        if (student.isEmpty()) {
            student = studentRepo.findByIdNumber(emailOrId);
        }
        if (student.isPresent() && passwordEncoder.matches(password, student.get().getPassword())) {
            if (!student.get().isEnabled()) { result.put("error", "Account not yet verified."); return result; }
            result.put("token", jwtUtils.generateJwtToken(student.get().getEmail(), "OJT_STUDENT"));
            result.put("role", "OJT_STUDENT");
            result.put("name", student.get().getFullName());
            result.put("studentId", student.get().getId());
            result.put("redirect", "/student/dashboard.html");
            return result;
        }

        result.put("error", "Invalid credentials.");
        return result;
    }



    public Map<String, Object> registerStudent(Map<String, String> data,
                                                MultipartFile idFront,
                                                MultipartFile idBack,
                                                MultipartFile profilePhoto) {
        Map<String, Object> result = new HashMap<>();

        if (studentRepo.existsByEmail(data.get("email"))) {
            result.put("error", "Email already registered."); return result;
        }

        Student student = new Student();
        student.setFullName(data.get("fullName"));
        student.setEmail(data.get("email"));
        student.setPassword(passwordEncoder.encode(data.get("password")));
        student.setRole(BaseUser.UserRole.OJT_STUDENT);
        student.setEnabled(true);
        student.setApproved(true);
        student.setRequiredHours(Double.parseDouble(data.get("requiredHours")));


        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            student.setProfilePhotoPath(fileStorage.saveFile(profilePhoto, "profiles"));
        }

        boolean fromSchool = Boolean.parseBoolean(data.get("fromSchool"));
        student.setFromSchool(fromSchool);

        if (fromSchool) {
            student.setIdNumber(data.get("idNumber"));
            student.setProgram(data.get("program"));
            student.setYearLevel(Integer.parseInt(data.getOrDefault("yearLevel", "4")));
            student.setSection(Student.Section.valueOf(data.get("section")));
        } else {
            student.setSchoolName(data.get("schoolName"));
            student.setPhoneNumber(data.get("phoneNumber"));
            if (idFront != null && !idFront.isEmpty()) {
                student.setSchoolIdFrontPath(fileStorage.saveFile(idFront, "ids"));
            }
            if (idBack != null && !idBack.isEmpty()) {
                student.setSchoolIdBackPath(fileStorage.saveFile(idBack, "ids"));
            }
        }

        studentRepo.save(student);
        result.put("success", true);
        result.put("message", "Account created successfully!");
        result.put("studentId", student.getId());
        return result;
    }



    public Map<String, Object> submitAdminRequest(Map<String, String> data) {
        Map<String, Object> result = new HashMap<>();

        if (adminRequestRepo.existsByEmail(data.get("email"))
                || adminRepo.existsByEmail(data.get("email"))) {
            result.put("error", "Email already submitted or registered."); return result;
        }

        AdminRequest req = new AdminRequest();
        req.setFullName(data.get("fullName"));
        req.setEmail(data.get("email"));
        req.setIdNumber(data.get("idNumber"));
        req.setInstitute(data.get("institute"));
        req.setHashedPassword(passwordEncoder.encode(data.get("password")));
        adminRequestRepo.save(req);

        result.put("success", true);
        result.put("message", "Admin request submitted! Awaiting approval.");
        return result;
    }



    public Map<String, Object> submitSuperAdminApplication(Map<String, String> data) {
        Map<String, Object> result = new HashMap<>();

        if (superAdminRepo.existsByEmail(data.get("email"))) {
            result.put("error", "Email already registered."); return result;
        }

        SuperAdmin sa = new SuperAdmin();
        sa.setFullName(data.get("fullName"));
        sa.setEmail(data.get("email"));
        sa.setPassword(passwordEncoder.encode(data.get("password")));
        sa.setIdNumber(data.get("idNumber"));
        sa.setPosition(data.get("position"));
        sa.setInstitute(data.get("institute"));
        sa.setReason(data.get("reason"));
        sa.setRole(BaseUser.UserRole.SUPERADMIN);
        sa.setEnabled(false);
        sa.setApproved(false);
        superAdminRepo.save(sa);

        result.put("success", true);
        result.put("message", "SuperAdmin application submitted! Awaiting approval.");
        return result;
    }



    private Optional<BaseUser> findUserByEmail(String email) {
        Optional<? extends BaseUser> s = studentRepo.findByEmail(email);
        if (s.isPresent()) return Optional.of(s.get());
        Optional<? extends BaseUser> a = adminRepo.findByEmail(email);
        if (a.isPresent()) return Optional.of(a.get());
        Optional<? extends BaseUser> sa = superAdminRepo.findByEmail(email);
        if (sa.isPresent()) return Optional.of(sa.get());
        return Optional.empty();
    }

    private void saveUser(BaseUser user) {
        if (user instanceof Student) studentRepo.save((Student) user);
        else if (user instanceof Admin) adminRepo.save((Admin) user);
        else if (user instanceof SuperAdmin) superAdminRepo.save((SuperAdmin) user);
    }



    @Transactional
    public Map<String, Object> forgotPassword(String email) {
        Map<String, Object> result = new HashMap<>();
        Optional<BaseUser> userOpt = findUserByEmail(email);

        if (userOpt.isEmpty()) {

            result.put("success", true);
            result.put("message", "If that email is registered, a reset link has been sent.");
            return result;
        }

        BaseUser user = userOpt.get();

        resetTokenRepo.deleteByEmail(user.getEmail());


        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user.getEmail(), 30);
        resetTokenRepo.save(resetToken);


        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        result.put("success", true);
        result.put("message", "If that email is registered, a reset link has been sent.");
        return result;
    }

    @Transactional
    public Map<String, Object> resetPassword(String token, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        Optional<PasswordResetToken> tokenOpt = resetTokenRepo.findByToken(token);

        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            result.put("error", "Invalid or expired reset token.");
            return result;
        }

        Optional<BaseUser> userOpt = findUserByEmail(tokenOpt.get().getEmail());
        if (userOpt.isEmpty()) {
            result.put("error", "User no longer exists.");
            return result;
        }

        BaseUser user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        saveUser(user);


        resetTokenRepo.delete(tokenOpt.get());

        result.put("success", true);
        result.put("message", "Password has been successfully reset! You can now log in.");
        return result;
    }
}
