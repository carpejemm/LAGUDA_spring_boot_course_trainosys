package com.trainosys.ecom.app.service;

import com.trainosys.ecom.app.dto.UserRequest;
import com.trainosys.ecom.app.dto.UserResponse;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserResponse> fetchAllUsers();
    void addUser(UserRequest userRequest);
    Optional<UserResponse> fetchUser(Long id);
    boolean updateUser(Long id, UserRequest updatedUserRequest);
}
