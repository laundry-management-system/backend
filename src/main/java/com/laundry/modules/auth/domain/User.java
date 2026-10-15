package com.laundry.modules.auth.domain;

import com.laundry.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
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
    Instant hiredDate;

    public boolean isActive(){
        return this.status.equals(Status.ACTIVE);
    }
}
