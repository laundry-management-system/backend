package com.laundry.modules.auth.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString(exclude = "passwordHash")
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "employee_code", nullable = false, unique = true, length = 10)
    String employeeCode;

    @Column(name = "full_name", nullable = false, length = 50)
    String fullName;

    @Column(name = "phone", nullable = false, unique = true,length = 10)
    String phone;

    @Column(name = "email", nullable = false, unique = true)
    String email;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    String username;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    Role role;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    Status status;

    @Column(name = "hired_date")
    LocalDate hiredDate;

    public boolean isActive(){
        return status.equals(Status.ACTIVE);
    }

    public void updateProfile(String fullName, String phone, LocalDate hiredDate){
        requireActive("cập nhật thông tin");
        this.fullName = fullName; this.phone = phone; this.hiredDate = hiredDate;
    }
    public void updateProfile(String fullName, String phone){
        requireActive("cập nhật thông tin");
        this.fullName = fullName; this.phone = phone;
    }
    public void disable(){ requireActive("vô hiệu hóa"); this.status = Status.INACTIVE; }

    public void enable(){
        if (status != Status.INACTIVE)
            throw new BusinessException("Chỉ tài khoản đã bị vô hiệu hóa mới có thể mở khóa");
        this.status = Status.ACTIVE;
    }

    private void requireActive(String action){
        switch (status) {
            case PENDING  -> throw new BusinessException("Tài khoản chưa kích hoạt, không thể " + action);
            case INACTIVE -> throw new BusinessException("Tài khoản đã bị vô hiệu hóa, không thể " + action);
            case ACTIVE   -> { /* Bỏ qua */ }
        }
    }

    public void requirePending(){
        switch (status) {
            case ACTIVE   -> throw new BusinessException("Tài khoản đã được kích hoạt");
            case INACTIVE -> throw new BusinessException("Tài khoản đã bị vô hiệu hóa");
            case PENDING  -> { /* Bỏ qua */ }
        }
    }

    public void setupPassword(String passwordHash){

        requirePending();

        this.status = Status.ACTIVE;
        this.passwordHash = passwordHash;
    }
}