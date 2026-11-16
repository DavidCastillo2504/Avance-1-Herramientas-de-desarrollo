package com.sabortradicion.app.repository;

import com.sabortradicion.app.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findAllByOrderByCategoryAscNameAsc();
    List<MenuItem> findByCategoryIgnoreCaseOrderByNameAsc(String category);
    long countByAvailableTrue();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select count(oi) > 0 from OrderItem oi where oi.menuItem.id = :menuItemId")
    boolean hasOrders(@Param("menuItemId") Long menuItemId);
}
