package com.civicdesk.auth.service;

import com.civicdesk.auth.dto.request.CreateDepartmentRequest;
import com.civicdesk.auth.dto.request.UpdateDepartmentRequest;
import com.civicdesk.auth.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    List<DepartmentResponse> getAll();

    DepartmentResponse getById(String id);

    DepartmentResponse createDepartment(CreateDepartmentRequest req);

    DepartmentResponse updateDepartment(String id, UpdateDepartmentRequest req);
}
