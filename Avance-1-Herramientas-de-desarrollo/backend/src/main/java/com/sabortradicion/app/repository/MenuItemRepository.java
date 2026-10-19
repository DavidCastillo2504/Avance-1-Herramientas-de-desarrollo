package com.sabortradicion.app.repository;

import com.sabortradicion.app.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findAllByOrderByCategoryAscNameAsc();
    List<MenuItem> findByCategoryIgnoreCaseOrderByNameAsc(String category);
    long countByAvailableTrue();
}
