package com.cashflow.demo.mapper;

import com.cashflow.demo.dto.user.UserRequest;
import com.cashflow.demo.dto.user.UserResponse;
import com.cashflow.demo.entity.UserModel;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    public UserMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public UserModel toEntity(UserRequest request){
        UserModel user = new UserModel();

        updateEntity(request, user);
        return user;
    }

    public void updateEntity(UserRequest request, UserModel user) {
        user.setName(request.name());
        user.setCnpj(request.cnpj());
        user.setSegment(request.segment());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(request.role());

    }

    public UserResponse response(UserModel user){
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getCnpj(),
                user.getSegment(),
                user.getEmail(),
                user.getPhone(),
                user.getRole()
        );
    }
}
