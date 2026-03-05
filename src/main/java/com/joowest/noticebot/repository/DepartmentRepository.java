package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {
    List<Department> findByEnabledTrueOrderBySortOrderAscDeptNameAsc();
    Optional<Department> findByDeptCode(String deptCode);
}
