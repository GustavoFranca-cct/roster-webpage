// repository/EmployeeRepository.java
package com.simpleroster.routegenerator.repository;

import com.simpleroster.routegenerator.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByName(String name); // Useful for checking duplicates

    // Method to find only active employees (New)
    List<Employee> findAllByIsActive(boolean isActive);

    // Method to count employees by active status (New)
    // refer to it using true for active employees and false for inactive employees
    long countByIsActive(boolean isActive);




    // Add custom query methods if needed later
}
