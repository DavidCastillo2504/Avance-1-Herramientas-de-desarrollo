package com.sabortradicion.app.repository;

import com.sabortradicion.app.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select coalesce(sum(o.total), 0) from CustomerOrder o")
    BigDecimal sumRegisteredSales();
}
