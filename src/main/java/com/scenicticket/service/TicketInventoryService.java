package com.scenicticket.service;

import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.TicketInventoryDAO;
import com.scenicticket.dao.mysql.TicketTypeDAO;
import com.scenicticket.dto.TicketAvailabilityDTO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Item;
import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;
import com.scenicticket.util.ConnectionProvider;
import com.scenicticket.util.MySQLDBUtil;
import com.scenicticket.util.SecurityUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TicketInventoryService {
    private final TicketTypeDAO ticketTypeDAO;
    private final TicketInventoryDAO inventoryDAO;
    private final ItemDAO itemDAO;
    private final AuthorizationService authorizationService;
    private final ConnectionProvider connectionProvider;

    public TicketInventoryService() {
        this(new TicketTypeDAO(), new TicketInventoryDAO(), new ItemDAO(), new AuthorizationService(),
                MySQLDBUtil::getConnection);
    }

    public TicketInventoryService(TicketTypeDAO ticketTypeDAO, TicketInventoryDAO inventoryDAO, ItemDAO itemDAO,
                                  AuthorizationService authorizationService, ConnectionProvider connectionProvider) {
        this.ticketTypeDAO = ticketTypeDAO;
        this.inventoryDAO = inventoryDAO;
        this.itemDAO = itemDAO;
        this.authorizationService = authorizationService;
        this.connectionProvider = connectionProvider;
    }

    public long createTicketType(long actorUserId, long itemId, String name, BigDecimal originalPrice,
                                 BigDecimal discountRate) {
        authorizationService.requireAdmin(actorUserId);
        requireItem(itemId);
        TicketType ticketType = new TicketType();
        ticketType.setItemId(itemId);
        ticketType.setName(SecurityUtil.requireText(name, "票种名称", 50));
        ticketType.setOriginalPrice(normalizePrice(originalPrice));
        ticketType.setDiscountRate(normalizeDiscount(discountRate));
        ticketType.setStatus(1);
        return ticketTypeDAO.create(ticketType);
    }

    public long createDefaultAdultTicketWithInventory(long actorUserId, long itemId,
                                                       BigDecimal price, BigDecimal discountRate,
                                                       LocalDate firstDate, int days, int stockPerDay) {
        authorizationService.requireAdmin(actorUserId);
        requireItem(itemId);
        if (firstDate == null || firstDate.isBefore(LocalDate.now())) {
            throw new BusinessException("默认库存开始日期必须是今天或未来日期");
        }
        if (days <= 0 || days > 366 || stockPerDay < 0 || stockPerDay > 1_000_000) {
            throw new BusinessException("默认库存天数或数量无效");
        }
        TicketType ticketType = new TicketType();
        ticketType.setItemId(itemId);
        ticketType.setName("成人票");
        ticketType.setOriginalPrice(normalizePrice(price));
        ticketType.setDiscountRate(normalizeDiscount(discountRate));
        ticketType.setStatus(1);
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long ticketTypeId = ticketTypeDAO.create(connection, ticketType);
                for (int offset = 0; offset < days; offset += 1) {
                    inventoryDAO.insert(connection, ticketTypeId, firstDate.plusDays(offset), stockPerDay);
                }
                connection.commit();
                return ticketTypeId;
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception instanceof RuntimeException runtimeException
                        ? runtimeException : new DBException("创建默认成人票和库存失败", exception));
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new DBException("创建默认成人票和库存失败", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DBException("默认票种库存事务失败", exception);
        }
    }

    public boolean updateTicketType(long actorUserId, long ticketTypeId, String name, BigDecimal originalPrice,
                                    BigDecimal discountRate, int status) {
        authorizationService.requireAdmin(actorUserId);
        TicketType ticketType = requireTicketType(ticketTypeId);
        if (status != 0 && status != 1) {
            throw new BusinessException("票种状态只能是上架或下架");
        }
        ticketType.setName(SecurityUtil.requireText(name, "票种名称", 50));
        ticketType.setOriginalPrice(normalizePrice(originalPrice));
        ticketType.setDiscountRate(normalizeDiscount(discountRate));
        ticketType.setStatus(status);
        return ticketTypeDAO.update(ticketType);
    }

    public List<TicketType> listTicketTypes(long actorUserId, long itemId, boolean includeInactive) {
        if (includeInactive) {
            authorizationService.requireAdmin(actorUserId);
        } else {
            authorizationService.requireActiveUser(actorUserId);
        }
        requireItem(itemId);
        return ticketTypeDAO.findByItemId(itemId, !includeInactive);
    }

    public List<TicketType> listAllTicketTypes(long actorUserId, boolean includeInactive) {
        if (includeInactive) {
            authorizationService.requireAdmin(actorUserId);
        } else {
            authorizationService.requireActiveUser(actorUserId);
        }
        return ticketTypeDAO.findAll(!includeInactive);
    }

    public List<TicketInventory> listInventory(long actorUserId, long ticketTypeId,
                                               LocalDate startDate, LocalDate endDate) {
        authorizationService.requireAdmin(actorUserId);
        requireTicketType(ticketTypeId);
        validateDateRange(startDate, endDate);
        return inventoryDAO.findByTicketType(ticketTypeId, startDate, endDate);
    }

    public TicketInventory setTotalStock(long actorUserId, long ticketTypeId, LocalDate visitDate, int totalStock) {
        authorizationService.requireAdmin(actorUserId);
        requireTicketType(ticketTypeId);
        requireMaintainableDate(visitDate);
        if (totalStock < 0 || totalStock > 1_000_000) {
            throw new BusinessException("总库存必须是 0 到 1000000 之间的整数");
        }
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                TicketInventory result = inventoryDAO.findForUpdate(connection, ticketTypeId, visitDate)
                        .map(current -> {
                            try {
                                inventoryDAO.updateTotal(connection, current, totalStock);
                                current.setTotalStock(totalStock);
                                current.setAvailableStock(totalStock - current.getReservedStock() - current.getSoldStock());
                                current.setVersion(current.getVersion() + 1);
                                return current;
                            } catch (SQLException exception) {
                                throw new DBException("更新每日库存失败", exception);
                            }
                        })
                        .orElseGet(() -> {
                            try {
                                inventoryDAO.insert(connection, ticketTypeId, visitDate, totalStock);
                                TicketInventory created = new TicketInventory();
                                created.setTicketTypeId(ticketTypeId);
                                created.setVisitDate(visitDate);
                                created.setTotalStock(totalStock);
                                created.setAvailableStock(totalStock);
                                created.setReservedStock(0);
                                created.setSoldStock(0);
                                created.setVersion(0);
                                return created;
                            } catch (SQLException exception) {
                                throw new DBException("创建每日库存失败", exception);
                            }
                        });
                connection.commit();
                return result;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DBException("每日库存事务失败", exception);
        }
    }

    public List<TicketAvailabilityDTO> listAvailable(long actorUserId, long itemId,
                                                     LocalDate startDate, LocalDate endDate) {
        authorizationService.requireActiveUser(actorUserId);
        Item item = requireItem(itemId);
        if (item.getStatus() == null || item.getStatus() != 1) {
            throw new BusinessException("景点当前未上架");
        }
        validateDateRange(startDate, endDate);
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException("游玩日期不能早于今天");
        }
        List<TicketAvailabilityDTO> result = new ArrayList<>();
        for (TicketType ticketType : ticketTypeDAO.findByItemId(itemId, true)) {
            for (TicketInventory inventory : inventoryDAO.findByTicketType(
                    ticketType.getTicketTypeId(), startDate, endDate)) {
                if (inventory.getAvailableStock() > 0) {
                    result.add(new TicketAvailabilityDTO(ticketType, inventory, discountedPrice(ticketType)));
                }
            }
        }
        return result;
    }

    public TicketInventory reserveStock(Connection connection, long ticketTypeId,
                                        LocalDate visitDate, int quantity) throws SQLException {
        if (ticketTypeId <= 0) {
            throw new BusinessException("票种ID必须大于 0");
        }
        TicketType ticketType = ticketTypeDAO.findById(connection, ticketTypeId)
                .orElseThrow(() -> new BusinessException("票种不存在"));
        if (ticketType.getStatus() == null || ticketType.getStatus() != 1) {
            throw new BusinessException("票种当前未上架");
        }
        if (visitDate == null || visitDate.isBefore(LocalDate.now())) {
            throw new BusinessException("游玩日期必须是今天或未来日期");
        }
        if (quantity <= 0 || quantity > 99) {
            throw new BusinessException("预留票数必须是 1 到 99 之间的整数");
        }
        return inventoryDAO.reserve(connection, ticketTypeId, visitDate, quantity);
    }

    public BigDecimal discountedPrice(TicketType ticketType) {
        BigDecimal multiplier = BigDecimal.valueOf(100).subtract(ticketType.getDiscountRate())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return ticketType.getOriginalPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private Item requireItem(long itemId) {
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        return itemDAO.findById(itemId).orElseThrow(() -> new BusinessException("景点不存在"));
    }

    private TicketType requireTicketType(long ticketTypeId) {
        if (ticketTypeId <= 0) {
            throw new BusinessException("票种ID必须大于 0");
        }
        return ticketTypeDAO.findById(ticketTypeId).orElseThrow(() -> new BusinessException("票种不存在"));
    }

    private void requireMaintainableDate(LocalDate visitDate) {
        if (visitDate == null || visitDate.isBefore(LocalDate.now())) {
            throw new BusinessException("只能维护今天或未来日期的库存");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("开始日期和结束日期不能为空");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        if (endDate.isAfter(startDate.plusDays(366))) {
            throw new BusinessException("单次库存查询范围不能超过 366 天");
        }
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new BusinessException("票价必须在 0 到 99999999.99 之间");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeDiscount(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("优惠减免比例必须在 0 到 100 之间");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void rollback(Connection connection, RuntimeException original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
