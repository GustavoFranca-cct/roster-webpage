// repository/ShiftRepository.java
package com.simpleroster.routegenerator.repository;

import com.simpleroster.routegenerator.entity.Employee;
import com.simpleroster.routegenerator.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByShiftDateBetweenOrderByShiftDateAscStartTimeAsc(LocalDate startDate, LocalDate endDate);

    // Method to clear old schedule before generating a new one for a specific period
    @Modifying
    @Query("DELETE FROM Shift s WHERE s.shiftDate >= :startDate AND s.shiftDate <= :endDate")
    void deleteByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Method to count shifts with no assigned employee (New)
    long countByEmployeeIsNull();

    // Method to count shifts with no assigned employee within a date range (Optional alternative)
    // long countByShiftDateBetweenAndEmployeeIsNull(LocalDate startDate, LocalDate endDate);

    // Find shifts for a specific employee on a specific date (New)
    List<Shift> findByEmployeeIdAndShiftDate(Long employeeId, LocalDate shiftDate);

    // Find shifts for a specific employee within a date range (New)
    List<Shift> findByEmployeeIdAndShiftDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    // Find shifts within a specific date range
    List<Shift> findByShiftDateBetween(LocalDate startDate, LocalDate endDate);
}