package com.scenicticket.ui;

import com.scenicticket.dto.TicketAvailabilityDTO;
import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketAvailabilityDialogPanelTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 13);

    @Test
    void defaultsToFutureRangeAndReturnsClickTimeSnapshot() {
        TicketAvailabilityDialogPanel panel = new TicketAvailabilityDialogPanel("拙政园", TODAY);

        TicketAvailabilityDialogPanel.DateRange request = panel.request();
        panel.setDates("2026-08-01", "2026-08-02");

        assertEquals(LocalDate.of(2026, 7, 14), request.startDate());
        assertEquals(LocalDate.of(2026, 7, 27), request.endDate());
    }

    @Test
    void rejectsPastReversedAndMalformedRangesBeforeQuery() {
        TicketAvailabilityDialogPanel panel = new TicketAvailabilityDialogPanel("拙政园", TODAY);
        panel.setDates("2026-07-12", "2026-07-20");
        assertThrows(IllegalArgumentException.class, panel::request);

        panel.setDates("2026-07-20", "2026-07-19");
        assertThrows(IllegalArgumentException.class, panel::request);

        assertThrows(IllegalArgumentException.class,
                () -> panel.setDates("2026/07/20", "2026-07-21"));
    }

    @Test
    void resultModelShowsAllBusinessColumnsAndIsReadOnly() {
        DefaultTableModel model = TicketAvailabilityDialogPanel.resultModel(List.of(option()));

        assertEquals(1, model.getRowCount());
        assertEquals(7, model.getColumnCount());
        assertEquals(19L, model.getValueAt(0, 0));
        assertEquals("学生票", model.getValueAt(0, 1));
        assertEquals(LocalDate.of(2026, 7, 20), model.getValueAt(0, 2));
        assertEquals("¥100.00", model.getValueAt(0, 3));
        assertEquals("减免20%", model.getValueAt(0, 4));
        assertEquals("¥80.00", model.getValueAt(0, 5));
        assertEquals(6, model.getValueAt(0, 6));
        assertEquals(false, model.isCellEditable(0, 0));
    }

    @Test
    void emptyResultsUseExplicitEmptyState() {
        JLabel label = assertInstanceOf(JLabel.class, TicketAvailabilityDialogPanel.results(List.of()));
        assertEquals("所选日期范围暂无可售票种或库存", label.getText());
    }

    private static TicketAvailabilityDTO option() {
        TicketType type = new TicketType();
        type.setTicketTypeId(19L);
        type.setName("学生票");
        type.setOriginalPrice(new BigDecimal("100.00"));
        type.setDiscountRate(new BigDecimal("20"));
        TicketInventory inventory = new TicketInventory();
        inventory.setTicketTypeId(19L);
        inventory.setVisitDate(LocalDate.of(2026, 7, 20));
        inventory.setAvailableStock(6);
        return new TicketAvailabilityDTO(type, inventory, new BigDecimal("80.00"));
    }
}
