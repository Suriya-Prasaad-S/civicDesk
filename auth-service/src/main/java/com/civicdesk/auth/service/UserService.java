package com.civicdesk.auth.service;

import com.civicdesk.auth.dto.request.CreateUserRequest;
import com.civicdesk.auth.dto.request.UpdateUserRequest;
import com.civicdesk.auth.dto.response.UserResponse;
import com.civicdesk.auth.response.PageResponse;

public interface UserService {

    UserResponse getById(String userId);

    UserResponse createUser(CreateUserRequest req, String callerRole, String callerUserId);

    UserResponse updateUser(String userId, UpdateUserRequest req, String callerRole, String callerUserId);

    UserResponse updateStatus(String userId, String status);

    PageResponse<UserResponse> getUsers(String callerRole, String callerUserId,
                                        String roleFilter, String statusFilter, String departmentIdFilter,
                                        int page, int size);
}
