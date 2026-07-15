package com.scenicticket.ui;

import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.dto.AuditLogQuery;
import com.scenicticket.dto.HotItemRankingDTO;
import com.scenicticket.dto.LoginResult;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.OrderActionResult;
import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.dto.StatisticsReportDTO;
import com.scenicticket.dto.AdminUserDetailDTO;
import com.scenicticket.dto.AdminChangeResult;
import com.scenicticket.dto.AdmissionResult;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.dto.TicketAvailabilityDTO;
import com.scenicticket.dto.CommentListDTO;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import com.scenicticket.model.Order;
import com.scenicticket.model.User;
import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;
import com.scenicticket.model.Admission;
import com.scenicticket.service.BusinessService;
import com.scenicticket.service.CrossDatabaseQueryService;
import com.scenicticket.service.RecommendService;
import com.scenicticket.service.StatisticsService;
import com.scenicticket.service.SystemLogService;
import com.scenicticket.service.UserService;
import com.scenicticket.service.AdminUserService;
import com.scenicticket.service.TicketInventoryService;
import com.scenicticket.service.OrderLifecycleService;
import com.scenicticket.service.AdmissionService;
import com.scenicticket.service.CommentService;
import org.bson.Document;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class AppFrame extends JFrame {
    private static final Color BACKGROUND = UiTheme.BACKGROUND;
    private static final Font TITLE_FONT = UiTheme.TITLE_FONT;

    private final UserService userService = new UserService();
    private final BusinessService businessService = new BusinessService();
    private final CrossDatabaseQueryService crossDatabaseQueryService = new CrossDatabaseQueryService();
    private final RecommendService recommendService = new RecommendService();
    private final StatisticsService statisticsService = new StatisticsService();
    private final SystemLogService systemLogService = new SystemLogService();
    private final AdminUserService adminUserService = new AdminUserService();
    private final TicketInventoryService ticketInventoryService = new TicketInventoryService();
    private final OrderLifecycleService orderLifecycleService = new OrderLifecycleService();
    private final AdmissionService admissionService = new AdmissionService();
    private final CommentService commentService = new CommentService();

    private final JLabel userLabel = new JLabel("未登录");
    private final JLabel statusLabel = new JLabel("就绪");
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
    private final Map<Long, String> categoryNames = new LinkedHashMap<>();
    private final ItemDisplayFormatter itemDisplayFormatter = new ItemDisplayFormatter(
            categoryId -> categoryNames.getOrDefault(categoryId, "未分类"));
    private final SwingTaskRunner taskRunner;

    private User currentUser;
    private final SessionTaskGuard sessionTaskGuard = new SessionTaskGuard();

    public AppFrame() {
        setTitle("景点售票系统");
        setMinimumSize(new Dimension(1100, 680));
        setSize(1360, 840);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND);
        taskRunner = new SwingTaskRunner(this, statusLabel, sessionTaskGuard, this::showError);
        showLoginView("请输入账号密码登录");
    }

    private void showLoginView(String message) {
        sessionTaskGuard.advanceSession();
        currentUser = null;
        updateSessionLabel();
        statusLabel.setText(message);
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        add(createAuthHeader(), BorderLayout.NORTH);
        add(createLoginPanel(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private void showRegisterView() {
        sessionTaskGuard.advanceSession();
        statusLabel.setText("创建新用户账号");
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        add(createAuthHeader(), BorderLayout.NORTH);
        add(createRegisterPanel(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private void showSystemView() {
        getRootPane().setDefaultButton(null);
        tabs.removeAll();
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(createPages(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        switchTo("首页");
        revalidate();
        repaint();
    }

    private Component createAuthHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(22, 32, 16, 32));
        header.setBackground(Color.WHITE);

        JLabel title = new JLabel("景点售票系统");
        title.setFont(TITLE_FONT);
        JLabel subtitle = new JLabel("请先登录后进入系统");
        subtitle.setForeground(UiTheme.MUTED);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(subtitle);

        header.add(titleBlock, BorderLayout.WEST);
        return header;
    }

    private Component createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        header.setBackground(Color.WHITE);

        JLabel title = new JLabel("景点售票系统");
        title.setFont(TITLE_FONT);
        JLabel subtitle = new JLabel("景点门票与运营管理");
        subtitle.setForeground(UiTheme.MUTED);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(subtitle);

        JPanel sessionBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        sessionBlock.setOpaque(false);
        JButton logoutButton = secondaryButton("退出登录");
        logoutButton.addActionListener(event -> {
            User loggingOutUser = currentUser;
            runTask("退出登录", () -> userService.logout(loggingOutUser, "127.0.0.1"), auditRecorded ->
                    showLoginView(auditRecorded ? "已退出登录" : "已退出登录，审计日志写入失败"));
        });
        sessionBlock.add(userLabel);
        sessionBlock.add(logoutButton);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(sessionBlock, BorderLayout.EAST);
        return header;
    }

    private Component createPages() {
        List<String> visiblePages = UiNavigationPolicy.visiblePages(currentUser == null ? null : currentUser.getRole());
        if (visiblePages.contains("首页")) {
            tabs.addTab("首页", scrollPage(createHomePanel()));
        }
        if (visiblePages.contains("个人档案")) {
            tabs.addTab("个人档案", scrollPage(createProfilePanel()));
        }
        if (visiblePages.contains("景点浏览")) {
            tabs.addTab("景点浏览", scrollPage(createItemPanel()));
        }
        if (visiblePages.contains("我的订单")) {
            tabs.addTab("我的订单", scrollPage(createOrderPanel()));
        }
        if (visiblePages.contains("统计报表")) {
            tabs.addTab("统计报表", scrollPage(createReportPanel()));
        }
        if (visiblePages.contains("后台管理")) {
            tabs.addTab("后台管理", scrollPage(createManagePanel()));
        }
        if (visiblePages.contains("系统审计")) {
            tabs.addTab("系统审计", scrollPage(createAuditPanel()));
        }
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabs.setBackground(BACKGROUND);
        styleNavigationTabs();
        return tabs;
    }

    private Component createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        footer.setBackground(Color.WHITE);
        footer.add(statusLabel, BorderLayout.WEST);
        return footer;
    }

    private JPanel createHomePanel() {
        return new HomePanel(currentUser, this::switchTo, this::setStatus);
    }

    private JPanel createLoginPanel() {
        LoginPanel panel = new LoginPanel(
                (username, password) -> userService.login(username, password, "127.0.0.1"),
                result -> {
                    setCurrentUser(result);
                    showSystemView();
                },
                this::showRegisterView,
                this::setStatus,
                taskRunner
        );
        getRootPane().setDefaultButton(panel.defaultButton());
        return panel;
    }

    private JPanel createRegisterPanel() {
        RegisterPanel panel = new RegisterPanel(
                userService::register,
                userId -> showLoginView("注册成功，用户ID：" + userId + "，请登录"),
                () -> showLoginView("请输入账号密码登录"),
                this::setStatus,
                taskRunner
        );
        getRootPane().setDefaultButton(panel.defaultButton());
        return panel;
    }

    private JPanel createProfilePanel() {
        long actorUserId = requireCurrentUserId();
        return new ProfilePanel(actorUserId,
                () -> userService.getProfile(actorUserId, actorUserId),
                profile -> userService.updateProfile(actorUserId, profile),
                taskRunner);
    }

    private JPanel createItemPanel() {
        return new ScenicBrowsePanel(taskRunner, new ScenicBrowsePanel.Actions() {
            @Override
            public List<Category> loadCategories() {
                return businessService.listCategories();
            }

            @Override
            public void categoriesLoaded(List<Category> categories) {
                updateCategoryCache(categories);
            }

            @Override
            public List<Item> searchItems(String keyword, Long categoryId) {
                return businessService.searchItems(keyword, categoryId, 50, 0);
            }

            @Override
            public List<RecommendationDTO> recommendForUser() {
                return recommendService.recommendForUser(requireCurrentUserId(), 10);
            }

            @Override
            public List<RecommendationDTO> recommendTopRated() {
                return recommendService.recommendTopRatedItems(10);
            }

            @Override
            public List<RecommendationDTO> recommendHot() {
                return recommendService.recommendHotItems(null, null, 10);
            }

            @Override
            public Map<Long, Double> loadRatings(List<Long> itemIds) {
                return recommendService.ratingScores(itemIds);
            }

            @Override
            public CrossDatabaseItemDTO loadItemDetail(long itemId) {
                return crossDatabaseQueryService.getItemDetail(itemId, 8);
            }

            @Override
            public CommentListDTO loadComments(long itemId) {
                return commentService.listForItem(requireCurrentUserId(), itemId, 20);
            }

            @Override
            public String formatItemIntroduction(CrossDatabaseItemDTO dto) {
                return itemDisplayFormatter.formatIntroduction(dto);
            }

            @Override
            public String formatCommentViews(String itemTitle, CommentListDTO dto) {
                return itemDisplayFormatter.formatComments(itemTitle, dto);
            }

            @Override
            public void showTicketAvailability(Item item) {
                showTicketAvailabilityDialog(item);
            }

            @Override
            public void showPurchase(Item item, JTextArea detailArea) {
                showPurchaseDialog(item, detailArea);
            }

            @Override
            public void showComment(Item item, JTextArea commentsArea) {
                showCommentDialog(item, commentsArea);
            }

            @Override
            public void setStatus(String message) {
                AppFrame.this.setStatus(message);
            }
        });
    }

    private JPanel createOrderPanel() {
        long actorUserId = requireCurrentUserId();
        return new OrderPanel(actorUserId, isCurrentAdmin(), taskRunner, new OrderPanel.Actions() {
            @Override
            public List<OrderViewDTO> search(Long queryUserId, Long orderId, Integer status) {
                orderLifecycleService.expireDueOrders(100);
                return businessService.searchOrderViews(actorUserId, queryUserId, orderId, status, 50, 0);
            }

            @Override
            public OrderActionResult pay(long orderId) {
                return orderLifecycleService.pay(actorUserId, orderId, "127.0.0.1");
            }

            @Override
            public OrderActionResult cancelPending(long orderId) {
                return orderLifecycleService.cancelPending(actorUserId, orderId, "127.0.0.1");
            }

            @Override
            public OrderActionResult refund(long orderId, String reason) {
                return orderLifecycleService.refund(actorUserId, orderId, reason, "127.0.0.1");
            }

            @Override
            public String requestRefundReason() {
                return JOptionPane.showInputDialog(AppFrame.this, "请输入退款原因", "申请模拟退款",
                        JOptionPane.PLAIN_MESSAGE);
            }

            @Override
            public void setStatus(String message) {
                AppFrame.this.setStatus(message);
            }
        });
    }

    private JPanel createManagePanel() {
        long actorUserId = requireCurrentUserId();
        return new ManagementPanel(taskRunner, new ManagementPanel.Actions() {
            @Override
            public List<Category> listCategories() {
                return businessService.listCategories();
            }

            @Override
            public void categoriesLoaded(List<Category> categories) {
                updateCategoryCache(categories);
            }

            @Override
            public List<Item> searchItems(String keyword, Long categoryId) {
                return businessService.searchAllItemsForAdmin(actorUserId, keyword, categoryId, 100, 0);
            }

            @Override
            public Document itemDetail(long itemId) {
                return businessService.getItemDetailForAdmin(actorUserId, itemId);
            }

            @Override
            public void showCreateItemDialog(Runnable refreshItems) {
                AppFrame.this.showCreateItemDialog(refreshItems);
            }

            @Override
            public boolean updateItem(long itemId, String title, long categoryId) {
                return businessService.updateItem(actorUserId, itemId, title, categoryId);
            }

            @Override
            public boolean updateItemPricing(long itemId, BigDecimal price, BigDecimal discount) {
                return businessService.updateItemPricing(actorUserId, itemId, price, discount);
            }

            @Override
            public boolean updateItemStatus(long itemId, int status) {
                return businessService.updateItemStatus(actorUserId, itemId, status);
            }

            @Override
            public boolean updateItemDetail(long itemId, String description, String images, String metadata) {
                return businessService.updateItemDetail(actorUserId, itemId, description,
                        UiInputParsers.imageLines(images), UiInputParsers.metadataDocument(metadata));
            }

            @Override
            public boolean updateItemComplete(long itemId, String title, long categoryId,
                                              BigDecimal price, BigDecimal discount, int status,
                                              String description, List<String> images, Document metadata) {
                return businessService.updateItemComplete(actorUserId, itemId, title, categoryId,
                        price, discount, status, description, images, metadata);
            }

            @Override
            public long createCategory(String name, Long parentId) {
                return businessService.createCategory(actorUserId, name, parentId);
            }

            @Override
            public boolean updateCategory(long categoryId, String name, Long parentId) {
                return businessService.updateCategory(actorUserId, categoryId, name, parentId);
            }

            @Override
            public void setStatus(String message) {
                AppFrame.this.setStatus(message);
            }
        }, createUserManagementPanel(), createTicketInventoryManagementPanel(), createAdmissionManagementPanel());
    }

    private JPanel createUserManagementPanel() {
        long actorUserId = requireCurrentUserId();
        return new UserManagementPanel(actorUserId, taskRunner, new UserManagementPanel.Actions() {
            @Override
            public List<User> search(UserSearchCriteria criteria) {
                return adminUserService.searchUsers(actorUserId, criteria);
            }

            @Override
            public AdminUserDetailDTO detail(long targetUserId) {
                return adminUserService.getUserDetail(actorUserId, targetUserId);
            }

            @Override
            public AdminChangeResult changeStatus(long targetUserId, int status, String reason) {
                return adminUserService.changeUserStatus(actorUserId, targetUserId, status, reason);
            }

            @Override
            public AdminChangeResult changeRole(long targetUserId, String role) {
                return adminUserService.changeUserRole(actorUserId, targetUserId, role);
            }

            @Override
            public void setStatus(String message) {
                AppFrame.this.setStatus(message);
            }
        });
    }

    private JPanel createAdmissionManagementPanel() {
        long actorUserId = requireCurrentUserId();
        return new AdmissionPanel(taskRunner, new AdmissionPanel.Actions() {
            @Override
            public List<Admission> listByOrder(long orderId) {
                return admissionService.listByOrder(actorUserId, orderId);
            }

            @Override
            public AdmissionResult admit(long orderId, int quantity, String note) {
                return admissionService.admit(actorUserId, orderId, quantity, note);
            }

            @Override
            public void setStatus(String message) {
                AppFrame.this.setStatus(message);
            }
        });
    }

    private JPanel createTicketInventoryManagementPanel() {
        long actorUserId = requireCurrentUserId();
        return new TicketInventoryPanel(taskRunner, new TicketInventoryPanel.Actions() {
            @Override
            public List<TicketType> listTypes(long itemId) {
                return ticketInventoryService.listTicketTypes(actorUserId, itemId, true);
            }

            @Override
            public List<TicketType> listAllTypes() {
                return ticketInventoryService.listAllTicketTypes(actorUserId, true);
            }

            @Override
            public long createType(long itemId, String name, BigDecimal price, BigDecimal discount) {
                return ticketInventoryService.createTicketType(actorUserId, itemId, name, price, discount);
            }

            @Override
            public boolean updateType(long ticketTypeId, String name, BigDecimal price,
                                      BigDecimal discount, int status) {
                return ticketInventoryService.updateTicketType(
                        actorUserId, ticketTypeId, name, price, discount, status);
            }

            @Override
            public List<TicketInventory> listInventory(long ticketTypeId, LocalDate startDate, LocalDate endDate) {
                return ticketInventoryService.listInventory(actorUserId, ticketTypeId, startDate, endDate);
            }

            @Override
            public TicketInventory setTotalStock(long ticketTypeId, LocalDate visitDate, int totalStock) {
                return ticketInventoryService.setTotalStock(actorUserId, ticketTypeId, visitDate, totalStock);
            }

            @Override
            public void setStatus(String message) {
                AppFrame.this.setStatus(message);
            }
        });
    }

    private JPanel createReportPanel() {
        long actorUserId = requireCurrentUserId();
        return new ReportPanel(actorUserId, isCurrentAdmin(), taskRunner, new ReportPanel.Actions() {
            @Override
            public List<MonthlyOrderReportDTO> monthly(int year, int month) {
                return statisticsService.getMonthlyOrderReport(year, month);
            }

            @Override
            public List<HotItemRankingDTO> hot() {
                return statisticsService.getHotItemRanking(null, null, 10);
            }

            @Override
            public Document userReport(long targetUserId) {
                return statisticsService.getUserReport(actorUserId, targetUserId, null, null);
            }

            @Override
            public StatisticsReportDTO dashboard(int year, int month) {
                return statisticsService.buildDashboardReport(actorUserId, null, null, year, month);
            }

            @Override
            public void setStatus(String message) {
                AppFrame.this.setStatus(message);
            }
        });
    }

    private JPanel createAuditPanel() {
        long actorUserId = requireCurrentUserId();
        return new AuditPanel(taskRunner, new AuditPanel.Actions() {
            @Override
            public List<Document> query(AuditLogQuery query) {
                return systemLogService.queryAuditLogs(actorUserId, query);
            }

            @Override
            public List<Document> summary(Date startTime, Date endTime) {
                return systemLogService.getAuditSummary(actorUserId, startTime, endTime);
            }

            @Override
            public List<Document> trend(Date startTime, Date endTime) {
                return systemLogService.getDailyAuditTrend(actorUserId, startTime, endTime);
            }

            @Override
            public List<Document> userSummary(Date startTime, Date endTime, int limit) {
                return systemLogService.getUserOperationSummary(actorUserId, startTime, endTime, limit);
            }

            @Override
            public void setStatus(String message) {
                AppFrame.this.setStatus(message);
            }
        });
    }

    private void setCurrentUser(LoginResult result) {
        sessionTaskGuard.advanceSession();
        currentUser = result.getUser();
        updateSessionLabel();
        setStatus("登录成功：" + currentUser.getUsername());
    }

    private void updateSessionLabel() {
        if (currentUser == null) {
            userLabel.setText("未登录");
            return;
        }
        userLabel.setText(currentUser.getUsername() + "  ·  " + roleDisplay(currentUser.getRole()));
    }

    private void styleNavigationTabs() {
        for (int i = 0; i < tabs.getTabCount(); i += 1) {
            JLabel label = new JLabel(tabs.getTitleAt(i));
            label.setForeground(UiTheme.TEXT);
            label.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            label.setPreferredSize(new Dimension(112, 38));
            tabs.setTabComponentAt(i, label);
        }
    }

    private void updateCategoryCache(List<Category> categories) {
        categoryNames.clear();
        for (Category category : categories) {
            categoryNames.put(category.getCategoryId(), category.getName());
        }
    }

    private void showCreateItemDialog(Runnable refreshItems) {
        if (categoryNames.isEmpty()) {
            showError(new IllegalStateException("景点类型尚未加载，请先刷新分类"));
            return;
        }
        long actorUserId = requireCurrentUserId();
        CreateItemDialogPanel form = new CreateItemDialogPanel(new LinkedHashMap<>(categoryNames));
        int result = JOptionPane.showConfirmDialog(this, form, "新增景点",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        CreateItemDialogPanel.CreateItemRequest request;
        try {
            request = form.request();
        } catch (RuntimeException exception) {
            showError(exception);
            return;
        }
        runAdminTask("新增景点", () -> {
            long id = businessService.createItem(actorUserId, request.title(), request.categoryId(),
                    request.description(), request.images(), request.metadata(), request.price(), request.discountRate());
            try {
                ticketInventoryService.createDefaultAdultTicketWithInventory(actorUserId, id,
                        request.price(), request.discountRate(), LocalDate.now(), 31, 100);
            } catch (RuntimeException provisioningFailure) {
                try {
                    businessService.updateItemStatus(actorUserId, id, 0);
                } catch (RuntimeException compensationFailure) {
                    provisioningFailure.addSuppressed(compensationFailure);
                }
                throw new IllegalStateException("景点已创建但默认成人票库存创建失败，景点已自动下架", provisioningFailure);
            }
            return id;
        }, id -> {
            setStatus("景点创建成功，编号：" + id);
            refreshItems.run();
        });
    }

    private void showTicketAvailabilityDialog(Item item) {
        long actorUserId = requireCurrentUserId();
        TicketAvailabilityDialogPanel form = new TicketAvailabilityDialogPanel(item.getTitle(), LocalDate.now());
        int result = JOptionPane.showConfirmDialog(this, form, "可售票种与日期",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        TicketAvailabilityDialogPanel.DateRange request;
        try {
            request = form.request();
        } catch (RuntimeException exception) {
            showError(exception);
            return;
        }
        runTask("查询可售票种与日期", () -> ticketInventoryService.listAvailable(actorUserId, item.getItemId(),
                request.startDate(), request.endDate()), options -> JOptionPane.showMessageDialog(this,
                TicketAvailabilityDialogPanel.results(options), "可售票种与日期", JOptionPane.INFORMATION_MESSAGE));
    }

    private void showPurchaseDialog(Item item, JTextArea detailArea) {
        if (item.getStatus() == null || item.getStatus() != 1) {
            showError(new IllegalArgumentException("该景点当前未上架，暂不能购买"));
            return;
        }
        long actorUserId = requireCurrentUserId();
        runTask("加载可售票种", () -> ticketInventoryService.listAvailable(actorUserId, item.getItemId(),
                LocalDate.now(), LocalDate.now().plusDays(30)), options -> {
            if (options.isEmpty()) {
                showError(new IllegalArgumentException("未来 30 天暂无可售票种或库存"));
                return;
            }
            showPendingOrderDialog(actorUserId, item, detailArea, options);
        });
    }

    private void showPendingOrderDialog(long actorUserId, Item item, JTextArea detailArea,
                                        List<TicketAvailabilityDTO> options) {
        PurchaseDialogPanel form = new PurchaseDialogPanel(item.getTitle(), options);
        int result = JOptionPane.showConfirmDialog(this, form, "创建待支付订单",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        PurchaseDialogPanel.PendingOrderRequest request = form.request();
        runTask("创建待支付订单", () -> orderLifecycleService.createPendingOrder(actorUserId,
                request.ticketTypeId(), request.visitDate(), request.quantity(), request.paymentMethod(),
                "127.0.0.1"), action -> detailArea.setText(
                PurchaseDialogPanel.successText(item.getTitle(), action)));
    }

    private void showCommentDialog(Item item, JTextArea detailArea) {
        long actorUserId = requireCurrentUserId();
        CommentDialogPanel form = new CommentDialogPanel(item.getTitle());
        int result = JOptionPane.showConfirmDialog(this, form, "发表评论",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        CommentDialogPanel.CommentSubmission submission = form.submission();
        runTask("发表评论", () -> commentService.submit(actorUserId, item.getItemId(),
                submission.content(), submission.rating(), submission.tags(), "127.0.0.1"), resultMessage -> {
            setStatus(resultMessage.message());
            if (!resultMessage.auditRecorded()) {
                JOptionPane.showMessageDialog(this, resultMessage.message(),
                        "评论已保存（审计警告）", JOptionPane.WARNING_MESSAGE);
            }
            runTask("刷新游客评论", () -> commentService.listForItem(
                    actorUserId, item.getItemId(), 20),
                    dto -> {
                        detailArea.setText(itemDisplayFormatter.formatComments(item.getTitle(), dto));
                        setStatus(resultMessage.message());
                    });
        });
    }

    private JScrollPane scrollPage(Component content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        return scrollPane;
    }

    private JButton secondaryButton(String text) {
        return UiComponents.secondaryButton(text);
    }

    private <T> void runAdminTask(String name, Callable<T> task, Consumer<T> onSuccess) {
        if (!isCurrentAdmin()) {
            showError(new IllegalStateException("请先使用管理员账号登录"));
            return;
        }
        runTask(name, task, onSuccess);
    }

    private <T> void runTask(String name, Callable<T> task, Consumer<T> onSuccess) {
        taskRunner.run(name, task, onSuccess);
    }

    private void showError(Throwable throwable) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                this,
                UiFormatters.chineseError(throwable),
                "操作失败",
                JOptionPane.ERROR_MESSAGE
        ));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private String roleDisplay(String role) {
        return UiFormatters.role(role);
    }

    private boolean isCurrentAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.getRole());
    }

    private void switchTo(String title) {
        for (int i = 0; i < tabs.getTabCount(); i += 1) {
            if (title.equals(tabs.getTitleAt(i))) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
    }

    private long requireCurrentUserId() {
        if (currentUser == null || currentUser.getUserId() == null || currentUser.getUserId() <= 0) {
            throw new IllegalStateException("请先登录或填写用户ID");
        }
        return currentUser.getUserId();
    }

}
