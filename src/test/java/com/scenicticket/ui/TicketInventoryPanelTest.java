package com.scenicticket.ui;

import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketInventoryPanelTest {
    @Test
    void selectingTypeFillsFormInventoryIdAndEnablesUpdate() {
        FakeActions actions = new FakeActions();
        actions.types = List.of(type(15L, 3L, "成人票", "120", "20", 1));
        TicketInventoryPanel panel = new TicketInventoryPanel(new ImmediateTaskExecutor(), actions);

        click(panel, "查询票种");
        assertTrue(actions.allTypesQueried);
        assertFalse(panel.updateEnabled());
        panel.selectTypeRow(0);

        assertTrue(panel.updateEnabled());
        assertEquals("成人票", panel.selectedTypeName());
        assertEquals("15", panel.inventoryTypeId());
        assertEquals(1, panel.typeRowCount());
    }

    @Test
    void createAndUpdateUseParsedSnapshotAndPreserveResultStatus() {
        FakeActions actions = new FakeActions();
        actions.types = List.of(type(15L, 3L, "成人票", "120", "20", 1));
        TicketInventoryPanel panel = new TicketInventoryPanel(new ImmediateTaskExecutor(), actions);
        panel.setTypeForm("3", "学生票", "80.00", "10", 1);

        click(panel, "新增票种");
        assertEquals(3L, actions.createdItemId);
        assertEquals("学生票", actions.createdName);
        assertEquals(new BigDecimal("80.00"), actions.createdPrice);
        assertEquals("票种创建成功，编号：99", last(actions.statuses));

        panel.selectTypeRow(0);
        panel.setTypeForm("3", "成人优惠票", "100", "15", 0);
        click(panel, "更新所选票种");

        assertEquals(15L, actions.updatedTypeId);
        assertEquals("成人优惠票", actions.updatedName);
        assertEquals(new BigDecimal("15"), actions.updatedDiscount);
        assertEquals(0, actions.updatedStatus);
        assertEquals("票种已更新", last(actions.statuses));
    }

    @Test
    void saveInventoryUsesDateRangeSnapshotRefreshesRowsAndKeepsSuccessMessage() {
        FakeActions actions = new FakeActions();
        TicketInventory stored = inventory(5L, 15L, LocalDate.of(2026, 7, 20), 100, 75, 10, 15, 2);
        actions.inventory = List.of(stored);
        actions.savedInventory = stored;
        TicketInventoryPanel panel = new TicketInventoryPanel(new ImmediateTaskExecutor(), actions);
        panel.setInventoryForm("15", "2026-07-20", "2026-07-25", "2026-07-20", "100");

        click(panel, "设置总库存");

        assertEquals(15L, actions.savedTypeId);
        assertEquals(LocalDate.of(2026, 7, 20), actions.savedDate);
        assertEquals(100, actions.savedTotalStock);
        assertEquals(LocalDate.of(2026, 7, 20), actions.listStart);
        assertEquals(LocalDate.of(2026, 7, 25), actions.listEnd);
        assertEquals(1, panel.inventoryRowCount());
        assertEquals(100, panel.inventoryValueAt(0, 3));
        assertEquals(75, panel.inventoryValueAt(0, 4));
        assertEquals(10, panel.inventoryValueAt(0, 5));
        assertEquals(15, panel.inventoryValueAt(0, 6));
        assertEquals(2, panel.inventoryValueAt(0, 7));
        assertEquals("库存已保存，可售 75 张", last(actions.statuses));
    }

    private static String last(List<String> values) {
        return values.get(values.size() - 1);
    }

    private static TicketType type(long id, long itemId, String name,
                                   String price, String discount, int status) {
        TicketType type = new TicketType();
        type.setTicketTypeId(id);
        type.setItemId(itemId);
        type.setName(name);
        type.setOriginalPrice(new BigDecimal(price));
        type.setDiscountRate(new BigDecimal(discount));
        type.setStatus(status);
        return type;
    }

    private static TicketInventory inventory(long id, long typeId, LocalDate date, int total,
                                             int available, int reserved, int sold, int version) {
        TicketInventory inventory = new TicketInventory();
        inventory.setInventoryId(id);
        inventory.setTicketTypeId(typeId);
        inventory.setVisitDate(date);
        inventory.setTotalStock(total);
        inventory.setAvailableStock(available);
        inventory.setReservedStock(reserved);
        inventory.setSoldStock(sold);
        inventory.setVersion(version);
        return inventory;
    }

    private static void click(Container root, String text) {
        JButton button = findButton(root, text);
        assertNotNull(button);
        button.doClick();
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static final class ImmediateTaskExecutor implements UiTaskExecutor {
        @Override
        public <T> void run(String name, Callable<T> task, Consumer<T> onSuccess) {
            try {
                onSuccess.accept(task.call());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static final class FakeActions implements TicketInventoryPanel.Actions {
        private List<TicketType> types = List.of();
        private List<TicketInventory> inventory = List.of();
        private TicketInventory savedInventory;
        private final List<String> statuses = new ArrayList<>();
        private long createdItemId;
        private String createdName;
        private BigDecimal createdPrice;
        private long updatedTypeId;
        private String updatedName;
        private BigDecimal updatedDiscount;
        private int updatedStatus;
        private long savedTypeId;
        private LocalDate savedDate;
        private int savedTotalStock;
        private LocalDate listStart;
        private LocalDate listEnd;
        private boolean allTypesQueried;

        @Override
        public List<TicketType> listTypes(long itemId) {
            return types;
        }

        @Override
        public List<TicketType> listAllTypes() {
            allTypesQueried = true;
            return types;
        }

        @Override
        public long createType(long itemId, String name, BigDecimal price, BigDecimal discount) {
            createdItemId = itemId;
            createdName = name;
            createdPrice = price;
            return 99L;
        }

        @Override
        public boolean updateType(long ticketTypeId, String name, BigDecimal price,
                                  BigDecimal discount, int status) {
            updatedTypeId = ticketTypeId;
            updatedName = name;
            updatedDiscount = discount;
            updatedStatus = status;
            return true;
        }

        @Override
        public List<TicketInventory> listInventory(long ticketTypeId, LocalDate startDate, LocalDate endDate) {
            listStart = startDate;
            listEnd = endDate;
            return inventory;
        }

        @Override
        public TicketInventory setTotalStock(long ticketTypeId, LocalDate visitDate, int totalStock) {
            savedTypeId = ticketTypeId;
            savedDate = visitDate;
            savedTotalStock = totalStock;
            return savedInventory;
        }

        @Override
        public void setStatus(String message) {
            statuses.add(message);
        }
    }
}
