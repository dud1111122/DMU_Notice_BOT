package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByEnabledTrueOrderBySortOrderAscDeptNameAsc();
    Optional<Department> findByDeptCode(String deptCode);
}
