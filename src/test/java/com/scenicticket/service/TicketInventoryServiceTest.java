package com.scenicticket.service;

import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.TicketInventoryDAO;
import com.scenicticket.dao.mysql.TicketTypeDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Item;
import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;
import com.scenicticket.model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketInventoryServiceTest {
    @Test
    void createsTicketTypeWithNormalizedPriceAndDiscount() {
        FakeTicketTypeDAO ticketTypes = new FakeTicketTypeDAO();
        TicketInventoryService service = service(ticketTypes, new FakeInventoryDAO(), TrackingConnection.create());

        assertEquals(88L, service.createTicketType(1L, 7L, "  学生票  ",
                new BigDecimal("58"), new BigDecimal("12.5")));

        assertEquals("学生票", ticketTypes.created.getName());
        assertEquals(new BigDecimal("58.00"), ticketTypes.created.getOriginalPrice());
        assertEquals(new BigDecimal("12.50"), ticketTypes.created.getDiscountRate());
    }

    @Test
    void stockUpdateUsesTransactionAndKeepsCommittedQuantity() {
        FakeInventoryDAO inventories = new FakeInventoryDAO();
        inventories.current = inventory(100, 60, 25, 15);
        TrackingConnection connection = TrackingConnection.create();
        TicketInventoryService service = service(new FakeTicketTypeDAO(), inventories, connection);

        TicketInventory updated = service.setTotalStock(1L, 9L, LocalDate.now().plusDays(2), 120);

        assertEquals(80, updated.getAvailableStock());
        assertEquals(120, inventories.updatedTotal);
        assertTrue(connection.committed);
        assertFalse(connection.rolledBack);
        assertEquals(List.of(false, true), connection.autoCommitValues);
    }

    @Test
    void stockCannotShrinkBelowReservedAndSoldAndRollsBack() {
        FakeInventoryDAO inventories = new FakeInventoryDAO();
        inventories.current = inventory(100, 60, 25, 15);
        TrackingConnection connection = TrackingConnection.create();
        TicketInventoryService service = service(new FakeTicketTypeDAO(), inventories, connection);

        assertThrows(BusinessException.class,
                () -> service.setTotalStock(1L, 9L, LocalDate.now().plusDays(2), 39));

        assertFalse(connection.committed);
        assertTrue(connection.rolledBack);
    }

    @Test
    void availabilityExcludesZeroStockAndCalculatesDiscountedPrice() {
        FakeInventoryDAO inventories = new FakeInventoryDAO();
        inventories.list = List.of(inventory(10, 0, 0, 10), inventory(10, 4, 0, 6));
        TicketInventoryService service = service(new FakeTicketTypeDAO(), inventories, TrackingConnection.create());

        var available = service.listAvailable(2L, 7L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertEquals(1, available.size());
        assertEquals(new BigDecimal("72.00"), available.get(0).discountedPrice());
    }

    @Test
    void defaultAdultTicketCreatesThirtyOneDaysOfOneHundredStockInOneTransaction() {
        FakeTicketTypeDAO ticketTypes = new FakeTicketTypeDAO();
        FakeInventoryDAO inventories = new FakeInventoryDAO();
        TrackingConnection connection = TrackingConnection.create();
        TicketInventoryService service = service(ticketTypes, inventories, connection);
        LocalDate today = LocalDate.now();

        long typeId = service.createDefaultAdultTicketWithInventory(1L, 7L,
                new BigDecimal("88"), new BigDecimal("10"), today, 31, 100);

        assertEquals(88L, typeId);
        assertEquals("成人票", ticketTypes.created.getName());
        assertEquals(new BigDecimal("88.00"), ticketTypes.created.getOriginalPrice());
        assertEquals(31, inventories.insertedDates.size());
        assertEquals(today, inventories.insertedDates.get(0));
        assertEquals(today.plusDays(30), inventories.insertedDates.get(30));
        assertTrue(inventories.insertedStocks.stream().allMatch(stock -> stock == 100));
        assertTrue(connection.committed);
    }

    private static TicketInventoryService service(FakeTicketTypeDAO ticketTypes, FakeInventoryDAO inventories,
                                                  TrackingConnection connection) {
        return new TicketInventoryService(ticketTypes, inventories, new FakeItemDAO(), allowAuthorization(),
                () -> connection.connection);
    }

    private static AuthorizationService allowAuthorization() {
        return new AuthorizationService() {
            @Override
            public User requireAdmin(long actorUserId) { return user(actorUserId, "ADMIN"); }
            @Override
            public User requireActiveUser(long actorUserId) { return user(actorUserId, "USER"); }
        };
    }

    private static User user(long id, String role) {
        User user = new User();
        user.setUserId(id);
        user.setRole(role);
        user.setStatus(1);
        return user;
    }

    private static TicketInventory inventory(int total, int available, int reserved, int sold) {
        TicketInventory inventory = new TicketInventory();
        inventory.setInventoryId(5L);
        inventory.setTicketTypeId(9L);
        inventory.setVisitDate(LocalDate.now().plusDays(2));
        inventory.setTotalStock(total);
        inventory.setAvailableStock(available);
        inventory.setReservedStock(reserved);
        inventory.setSoldStock(sold);
        inventory.setVersion(0);
        return inventory;
    }

    private static class FakeItemDAO extends ItemDAO {
        @Override
        public Optional<Item> findById(long itemId) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setStatus(1);
            return Optional.of(item);
        }
    }

    private static class FakeTicketTypeDAO extends TicketTypeDAO {
        private TicketType created;

        @Override
        public long create(TicketType ticketType) {
            created = ticketType;
            return 88L;
        }

        @Override
        public long create(Connection connection, TicketType ticketType) {
            return create(ticketType);
        }

        @Override
        public Optional<TicketType> findById(long ticketTypeId) {
            return Optional.of(ticketType(ticketTypeId));
        }

        @Override
        public List<TicketType> findByItemId(long itemId, boolean activeOnly) {
            return List.of(ticketType(9L));
        }

        private TicketType ticketType(long id) {
            TicketType ticketType = new TicketType();
            ticketType.setTicketTypeId(id);
            ticketType.setItemId(7L);
            ticketType.setName("成人票");
            ticketType.setOriginalPrice(new BigDecimal("80.00"));
            ticketType.setDiscountRate(new BigDecimal("10.00"));
            ticketType.setStatus(1);
            return ticketType;
        }
    }

    private static class FakeInventoryDAO extends TicketInventoryDAO {
        private TicketInventory current;
        private List<TicketInventory> list = List.of();
        private int updatedTotal;
        private final List<LocalDate> insertedDates = new ArrayList<>();
        private final List<Integer> insertedStocks = new ArrayList<>();

        @Override
        public void insert(Connection connection, long ticketTypeId, LocalDate visitDate, int totalStock) {
            insertedDates.add(visitDate);
            insertedStocks.add(totalStock);
        }

        @Override
        public Optional<TicketInventory> findForUpdate(Connection connection, long ticketTypeId,
                                                       LocalDate visitDate) {
            return Optional.ofNullable(current);
        }

        @Override
        public void updateTotal(Connection connection, TicketInventory current, int totalStock) {
            int committed = current.getReservedStock() + current.getSoldStock();
            if (totalStock < committed) {
                throw new BusinessException("库存不足以覆盖已预留和已售数量");
            }
            updatedTotal = totalStock;
        }

        @Override
        public List<TicketInventory> findByTicketType(long ticketTypeId, LocalDate startDate, LocalDate endDate) {
            return list;
        }
    }

    private static class TrackingConnection {
        private final List<Boolean> autoCommitValues = new ArrayList<>();
        private Connection connection;
        private boolean committed;
        private boolean rolledBack;

        private static TrackingConnection create() {
            TrackingConnection tracking = new TrackingConnection();
            tracking.connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit" -> { tracking.autoCommitValues.add((Boolean) args[0]); yield null; }
                        case "commit" -> { tracking.committed = true; yield null; }
                        case "rollback" -> { tracking.rolledBack = true; yield null; }
                        case "close" -> null;
                        case "isClosed" -> false;
                        case "unwrap" -> null;
                        case "isWrapperFor" -> false;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            return tracking;
        }
    }
}
