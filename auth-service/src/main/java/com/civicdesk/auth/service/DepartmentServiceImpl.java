package com.civicdesk.auth.service;

import com.civicdesk.auth.dto.request.CreateDepartmentRequest;
import com.civicdesk.auth.dto.request.UpdateDepartmentRequest;
import com.civicdesk.auth.dto.response.DepartmentResponse;
import com.civicdesk.auth.entity.Department;
import com.civicdesk.auth.exception.BadRequestException;
import com.civicdesk.auth.repository.DepartmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll(Sort.by("name")).stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Override
    public DepartmentResponse getById(String id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Department not found"));
        return DepartmentResponse.from(department);
    }

    @Override
    public DepartmentResponse createDepartment(CreateDepartmentRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("Department name is required");
        }
        if (departmentRepository.existsByName(req.getName().trim())) {
            throw new BadRequestException("Department already exists");
        }
        Department department = new Department();
        department.setDepartmentId(nextDepartmentId());
        department.setName(req.getName().trim());
        departmentRepository.save(department);
        return DepartmentResponse.from(department);
    }

    private String nextDepartmentId() {
        int max = departmentRepository.findAll().stream()
                .map(Department::getDepartmentId)
                .filter(id -> id != null && id.matches("DPT\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(3)))
                .max()
                .orElse(0);
        return String.format("DPT%02d", max + 1);
    }

    @Override
    public DepartmentResponse updateDepartment(String id, UpdateDepartmentRequest req) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Department not found"));
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("Department name is required");
        }
        if (!department.getName().equalsIgnoreCase(req.getName().trim()) && departmentRepository.existsByName(req.getName().trim())) {
            throw new BadRequestException("Another department already uses that name");
        }
        department.setName(req.getName().trim());
        departmentRepository.save(department);
        return DepartmentResponse.from(department);
    }
}
