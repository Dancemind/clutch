package com.clutch.app.entity;

import com.clutch.app.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends CompanyBaseEntity {

    // system user has its own company dedicated UUID
    @Column(unique = true, nullable = false)
    private String email;

    private String password; // Nullable for user from Google OAuth2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // todo: add field for disabled user

}
