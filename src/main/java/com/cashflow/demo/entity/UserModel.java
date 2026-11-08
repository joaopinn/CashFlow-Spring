package com.cashflow.demo.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    private String name;

    @Column(length = 18)
    private String cnpj;

    private String segment;

    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    private String phone;

    private String role;
}
