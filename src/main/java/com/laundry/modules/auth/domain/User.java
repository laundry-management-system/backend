package com.laundry.modules.auth.domain;

import com.laundry.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
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
        return this.status.equals(Status.ACTIVE);
    }
    public void updateProfile(String fullName, String phone){
        this.fullName = fullName;
        this.phone = phone;
    }

    public void disable(){
        this.status = Status.INACTIVE;
    }

    public void enable(){
        this.status = Status.ACTIVE;
    }

    public void setupPassword(String passwordHash){
        this.passwordHash = passwordHash;
    }
}
