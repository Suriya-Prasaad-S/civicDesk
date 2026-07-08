package com.civicdesk.publicworks.repository;

import com.civicdesk.publicworks.entity.WorkOrder;
import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.enums.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findByStatus(WorkOrderStatus status);

    List<WorkOrder> findByDepartmentId(Long departmentId);

    List<WorkOrder> findByContractorId(Long contractorId);

    List<WorkOrder> findByCategory(WorkCategory category);

    List<WorkOrder> findByWard(String ward);

    List<WorkOrder> findByStatusAndDepartmentId(WorkOrderStatus status, Long departmentId);
    long countByStatus(
            WorkOrderStatus status);

    List<WorkOrder> findByWardAndIsDeletedFalse(
            String ward);
    List<WorkOrder> findByStatusAndIsDeletedFalse(
            WorkOrderStatus status);

    List<WorkOrder> findByIsDeletedFalse();

    // Budget utilisation: work orders where spend exceeds allocated
    @Query("SELECT w FROM WorkOrder w WHERE w.budgetConsumedTotal > w.budgetAllocated")
    List<WorkOrder> findBudgetOverruns();

    // Summary for compliance/admin
    @Query("SELECT w.status, COUNT(w), SUM(w.budgetAllocated), SUM(w.budgetConsumedTotal) " +
           "FROM WorkOrder w GROUP BY w.status")
    List<Object[]> budgetSummaryByStatus();

    // Department-level budget rollup
    @Query("SELECT SUM(w.budgetAllocated), SUM(w.budgetConsumedTotal) " +
           "FROM WorkOrder w WHERE w.departmentId = :deptId")
    List<Object[]> departmentBudgetSummary(@Param("deptId") Long deptId);



    @Query("""
SELECT COUNT(w)
FROM WorkOrder w
WHERE w.status <> 'COMPLETED'
AND w.expectedEndDate < CURRENT_DATE
""")
    Long countDelayedWorkOrders();
    @Query("""
SELECT COALESCE(SUM(w.budgetAllocated),0)
FROM WorkOrder w
""")
    BigDecimal getTotalAllocatedBudget();
    @Query("""
SELECT COALESCE(SUM(w.budgetConsumedTotal),0)
FROM WorkOrder w
""")
    BigDecimal getTotalConsumedBudget();

    @Query("""
SELECT w.category,
COUNT(w)
FROM WorkOrder w
GROUP BY w.category
""")
    List<Object[]> categoryBreakdown();

    @Query("""
SELECT w.ward,
COUNT(w)
FROM WorkOrder w
GROUP BY w.ward
""")
    List<Object[]> wardBreakdown();
}
