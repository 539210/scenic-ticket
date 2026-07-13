package com.scenicticket.ui;

import com.scenicticket.dto.PendingOrderResult;
import com.scenicticket.dto.TicketAvailabilityDTO;
import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseDialogPanelTest {
    @Test
    void selectionUpdatesEstimateAndClampsQuantityToAvailableStock() {
        PurchaseDialogPanel panel = new PurchaseDialogPanel("拙政园", List.of(
                option(11L, "成人票", "80.00", LocalDate.of(2026, 7, 20), 10),
                option(12L, "儿童票", "40.00", LocalDate.of(2026, 7, 21), 2)));

        panel.setQuantity(8);
        assertEquals("¥640.00", panel.estimatedAmountText());

        panel.selectOption(1);

        assertEquals(2, panel.maximumQuantity());
        assertEquals(2, panel.quantity());
        assertEquals("¥80.00", panel.estimatedAmountText());
    }

    @Test
    void requestIsAnImmutableSnapshotOfSelectedBusinessIdentifiers() {
        PurchaseDialogPanel panel = new PurchaseDialogPanel("拙政园", List.of(
                option(11L, "成人票", "80.00", LocalDate.of(2026, 7, 20), 10),
                option(12L, "学生票", "60.00", LocalDate.of(2026, 7, 22), 6)));
        panel.selectOption(1);
        panel.setQuantity(3);
        panel.setPaymentMethod("支付宝");

        PurchaseDialogPanel.PendingOrderRequest request = panel.request();
        panel.selectOption(0);
        panel.setQuantity(1);

        assertEquals(12L, request.ticketTypeId());
        assertEquals(LocalDate.of(2026, 7, 22), request.visitDate());
        assertEquals(3, request.quantity());
        assertEquals("支付宝", request.paymentMethod());
    }

    @Test
    void successTextUsesServerAmountAndSnapshotsInsteadOfEarlierEstimate() {
        PendingOrderResult result = new PendingOrderResult(88L, new BigDecimal("175.00"),
                "调价后的学生票", LocalDate.of(2026, 7, 25), 2, true,
                "待支付订单已创建");

        String text = PurchaseDialogPanel.successText("狮子林", result);

        assertTrue(text.contains("服务端实际金额：¥175.00"));
        assertTrue(text.contains("票种：调价后的学生票"));
        assertTrue(text.contains("购买票数：2"));
        assertTrue(!text.contains("下单前估算金额"));
    }

    @Test
    void rejectsAvailabilityRowsWithoutPositiveStock() {
        assertThrows(IllegalArgumentException.class, () -> new PurchaseDialogPanel("拙政园", List.of(
                option(11L, "成人票", "80.00", LocalDate.of(2026, 7, 20), 0))));
    }

    private static TicketAvailabilityDTO option(long id, String name, String price,
                                                 LocalDate visitDate, int availableStock) {
        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(id);
        ticketType.setName(name);
        ticketType.setOriginalPrice(new BigDecimal(price));
        ticketType.setDiscountRate(BigDecimal.ZERO);
        ticketType.setStatus(1);
        TicketInventory inventory = new TicketInventory();
        inventory.setTicketTypeId(id);
        inventory.setVisitDate(visitDate);
        inventory.setAvailableStock(availableStock);
        return new TicketAvailabilityDTO(ticketType, inventory, new BigDecimal(price));
    }
}
