package com.scenicticket.dto;

import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;

import java.math.BigDecimal;

public record TicketAvailabilityDTO(TicketType ticketType, TicketInventory inventory,
                                    BigDecimal discountedPrice) {
}
