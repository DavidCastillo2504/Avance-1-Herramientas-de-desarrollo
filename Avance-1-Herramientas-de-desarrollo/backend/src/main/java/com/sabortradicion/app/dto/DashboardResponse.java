package com.sabortradicion.app.dto;

import java.math.BigDecimal;

public record DashboardResponse(long orderCount, long availableMenuItemCount, BigDecimal registeredSales) {
}
