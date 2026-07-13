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
import com.scenicticket.service.BehaviorLogService;
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
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class AppFrame extends JFrame {
    private static final Color BACKGROUND = UiTheme.BACKGROUND;
    private static final Color PANEL_BORDER = UiTheme.BORDER;
    private static final Font TITLE_FONT = UiTheme.TITLE_FONT;
    private static final Font SECTION_FONT = UiTheme.SECTION_FONT;

    private final UserService userService = new UserService();
    private final BusinessService businessService = new BusinessService();
    private final CrossDatabaseQueryService crossDatabaseQueryService = new CrossDatabaseQueryService();
    private final RecommendService recommendService = new RecommendService();
    private final StatisticsService statisticsService = new StatisticsService();
    private final SystemLogService systemLogService = new SystemLogService();
    private final BehaviorLogService behaviorLogService = new BehaviorLogService();
    private final AdminUserService adminUserService = new AdminUserService();
    private final TicketInventoryService ticketInventoryService = new TicketInventoryService();
    private final OrderLifecycleService orderLifecycleService = new OrderLifecycleService();
    private final AdmissionService admissionService = new AdmissionService();
    private final CommentService commentService = new CommentService();

    private final JLabel userLabel = new JLabel("未登录");
    private final JLabel statusLabel = new JLabel("就绪");
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
    private final Map<Long, String> categoryNames = new LinkedHashMap<>();
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
        JTextField loginUsername = new JTextField(22);
        JPasswordField loginPassword = new JPasswordField(22);
        JButton loginButton = primaryButton("登录");
        JButton registerPageButton = secondaryButton("注册新账号");
        JLabel loginResult = new JLabel(" ");

        JPanel loginPanel = formPanel("用户登录");
        loginPanel.setPreferredSize(new Dimension(420, 265));
        addField(loginPanel, 0, "用户名", loginUsername);
        addField(loginPanel, 1, "密码", loginPassword);
        addFormButton(loginPanel, 2, loginButton);
        addFormButton(loginPanel, 3, registerPageButton);
        addFormMessage(loginPanel, 4, loginResult);

        loginButton.addActionListener(event -> runTask("用户登录", () -> userService.login(
                loginUsername.getText(),
                new String(loginPassword.getPassword()),
                "127.0.0.1"
        ), result -> {
            loginResult.setText(result.getMessage());
            loginPassword.setText("");
            if (result.isSuccess()) {
                loginUsername.setText("");
                setCurrentUser(result);
                showSystemView();
            } else {
                loginResult.setForeground(UiTheme.DANGER);
                setStatus(result.getMessage());
            }
        }, message -> {
            loginResult.setForeground(UiTheme.DANGER);
            loginResult.setText(message);
        }));

        registerPageButton.addActionListener(event -> showRegisterView());
        getRootPane().setDefaultButton(loginButton);

        JPanel holder = pagePanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(24, 24, 24, 24);
        holder.add(loginPanel, gbc);
        return holder;
    }

    private JPanel createRegisterPanel() {
        JTextField registerUsername = new JTextField(22);
        JPasswordField registerPassword = new JPasswordField(22);
        JTextField registerEmail = new JTextField(22);
        JTextField registerPhone = new JTextField(22);
        JButton registerButton = primaryButton("注册");
        JButton backButton = secondaryButton("返回登录");
        JLabel registerResult = new JLabel(" ");

        JPanel registerPanel = formPanel("用户注册");
        registerPanel.setPreferredSize(new Dimension(460, 300));
        addField(registerPanel, 0, "用户名", registerUsername);
        addField(registerPanel, 1, "密码", registerPassword);
        addField(registerPanel, 2, "邮箱", registerEmail);
        addField(registerPanel, 3, "手机号", registerPhone);
        addFormButtons(registerPanel, 4, registerButton, backButton);
        addFormMessage(registerPanel, 5, registerResult);

        registerButton.addActionListener(event -> runTask("用户注册", () -> userService.register(
                registerUsername.getText(),
                new String(registerPassword.getPassword()),
                registerEmail.getText(),
                registerPhone.getText()
        ), userId -> {
            clearTextFields(registerPanel);
            showLoginView("注册成功，用户ID：" + userId + "，请登录");
        }, message -> {
            registerResult.setForeground(UiTheme.DANGER);
            registerResult.setText(message);
        }));

        backButton.addActionListener(event -> showLoginView("请输入账号密码登录"));
        getRootPane().setDefaultButton(registerButton);

        JPanel holder = pagePanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(24, 24, 24, 24);
        holder.add(registerPanel, gbc);
        return holder;
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
            public CrossDatabaseItemDTO loadItemDetail(long itemId) {
                return crossDatabaseQueryService.getItemDetail(itemId, 8);
            }

            @Override
            public CommentListDTO loadComments(long itemId) {
                return commentService.listForItem(requireCurrentUserId(), itemId, 20);
            }

            @Override
            public String formatItemIntroduction(CrossDatabaseItemDTO dto) {
                return AppFrame.this.formatItemIntroduction(dto);
            }

            @Override
            public String formatCommentViews(String itemTitle, CommentListDTO dto) {
                return AppFrame.this.formatCommentViews(itemTitle, dto);
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
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTabbedPane manageTabs = new JTabbedPane(JTabbedPane.TOP);

        JPanel itemPage = new JPanel(new BorderLayout(12, 12));
        itemPage.setOpaque(false);
        JTextField itemKeywordField = new JTextField(16);
        JComboBox<CategoryOption> itemCategoryBox = new JComboBox<>();
        itemCategoryBox.addItem(new CategoryOption("全部类型", null));
        DefaultTableModel itemModel = tableModel("ID", "景点名称", "类型", "原价", "优惠", "折后价", "状态", "更新时间");
        JTable itemTable = createTable(itemModel);
        itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setColumnWidths(itemTable, 55, 210, 115, 85, 90, 90, 70, 160);
        List<Item> adminItems = new ArrayList<>();
        Item[] selectedItem = new Item[1];

        JButton queryItemsButton = primaryButton("查询");
        JButton refreshItemsButton = secondaryButton("刷新");
        JButton resetItemsButton = secondaryButton("重置");
        JButton createItemButton = primaryButton("新增景点");
        JPanel itemQueryToolbar = toolbar();
        itemQueryToolbar.add(new JLabel("关键词"));
        itemQueryToolbar.add(itemKeywordField);
        itemQueryToolbar.add(new JLabel("景点类型"));
        itemQueryToolbar.add(itemCategoryBox);
        itemQueryToolbar.add(queryItemsButton);
        itemQueryToolbar.add(refreshItemsButton);
        itemQueryToolbar.add(resetItemsButton);
        itemQueryToolbar.add(createItemButton);

        JLabel selectedItemLabel = new JLabel("请先从表格选择景点");
        JTextField itemTitleField = new JTextField(14);
        JComboBox<CategoryOption> itemEditCategoryBox = new JComboBox<>();
        JTextField itemPriceField = new JTextField(8);
        JTextField itemDiscountField = new JTextField(6);
        JComboBox<String> itemStatusBox = new JComboBox<>(new String[]{"下架", "上架"});
        JButton basicItemButton = primaryButton("更新名称和分类");
        JButton pricingButton = primaryButton("更新票价和优惠");
        JButton itemStatusButton = secondaryButton("更新上下架");
        JTextArea itemIntroArea = new JTextArea(4, 48);
        itemIntroArea.setLineWrap(true);
        itemIntroArea.setWrapStyleWord(true);
        JTextArea itemImagesArea = new JTextArea(3, 48);
        JTextArea itemMetadataArea = new JTextArea(3, 48);
        itemImagesArea.setLineWrap(true);
        itemImagesArea.setWrapStyleWord(true);
        itemMetadataArea.setLineWrap(true);
        itemMetadataArea.setWrapStyleWord(true);
        JButton updateIntroButton = primaryButton("更新完整详情");
        basicItemButton.setEnabled(false);
        pricingButton.setEnabled(false);
        itemStatusButton.setEnabled(false);
        updateIntroButton.setEnabled(false);
        JPanel itemEditToolbar = toolbar();
        itemEditToolbar.add(selectedItemLabel);
        itemEditToolbar.add(new JLabel("名称"));
        itemEditToolbar.add(itemTitleField);
        itemEditToolbar.add(new JLabel("分类"));
        itemEditToolbar.add(itemEditCategoryBox);
        itemEditToolbar.add(basicItemButton);
        itemEditToolbar.add(new JLabel("门票原价"));
        itemEditToolbar.add(itemPriceField);
        JLabel discountLabel = new JLabel("优惠减免%");
        discountLabel.setToolTipText("例如填写20表示减免20%，即按原价的80%售票");
        itemEditToolbar.add(discountLabel);
        itemEditToolbar.add(itemDiscountField);
        itemEditToolbar.add(pricingButton);
        itemEditToolbar.add(new JLabel("状态"));
        itemEditToolbar.add(itemStatusBox);
        itemEditToolbar.add(itemStatusButton);

        JTabbedPane detailEditorTabs = new JTabbedPane();
        detailEditorTabs.addTab("景点简介", new JScrollPane(itemIntroArea));
        detailEditorTabs.addTab("图片地址（每行一个）", new JScrollPane(itemImagesArea));
        detailEditorTabs.addTab("扩展属性（JSON）", new JScrollPane(itemMetadataArea));
        JPanel introEditPanel = new JPanel(new BorderLayout(10, 0));
        introEditPanel.setOpaque(false);
        introEditPanel.add(detailEditorTabs, BorderLayout.CENTER);
        introEditPanel.add(updateIntroButton, BorderLayout.EAST);

        Runnable refreshItems = () -> runAdminTask("刷新景点", () -> businessService.searchAllItemsForAdmin(
                requireCurrentUserId(),
                itemKeywordField.getText(), selectedCategoryId(itemCategoryBox), 100, 0
        ), items -> {
            itemModel.setRowCount(0);
            adminItems.clear();
            adminItems.addAll(items);
            for (Item item : items) {
                itemModel.addRow(new Object[]{item.getItemId(), item.getTitle(), categoryName(item.getCategoryId()),
                        UiFormatters.money(item.getPrice()), discountText(item.getDiscountRate()),
                        UiFormatters.money(UiFormatters.discountedUnitPrice(item.getPrice(), item.getDiscountRate())),
                        formatItemStatus(item.getStatus()), formatDate(item.getUpdatedAt())});
            }
            selectedItem[0] = null;
            selectedItemLabel.setText("请先从表格选择景点");
            basicItemButton.setEnabled(false);
            pricingButton.setEnabled(false);
            itemStatusButton.setEnabled(false);
            updateIntroButton.setEnabled(false);
            itemIntroArea.setText("");
            itemImagesArea.setText("");
            itemMetadataArea.setText("");
            setStatus("已加载 " + items.size() + " 个景点");
        });

        itemTable.getSelectionModel().addListSelectionListener(event -> {
            int row = itemTable.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                selectedItem[0] = adminItems.get(itemTable.convertRowIndexToModel(row));
                Item item = selectedItem[0];
                selectedItemLabel.setText("已选择：" + item.getTitle());
                itemTitleField.setText(item.getTitle());
                selectCategory(itemEditCategoryBox, item.getCategoryId());
                itemPriceField.setText(item.getPrice() == null ? "0.00" : item.getPrice().toPlainString());
                itemDiscountField.setText(item.getDiscountRate() == null ? "0" : item.getDiscountRate().stripTrailingZeros().toPlainString());
                itemStatusBox.setSelectedIndex(item.getStatus() != null && item.getStatus() == 1 ? 1 : 0);
                pricingButton.setEnabled(true);
                basicItemButton.setEnabled(true);
                itemStatusButton.setEnabled(true);
                updateIntroButton.setEnabled(true);
                long requestedItemId = item.getItemId();
                runAdminTask("加载景点详情", () -> businessService.getItemDetailForAdmin(
                        requireCurrentUserId(), requestedItemId), detail -> {
                    if (isSelectedItem(selectedItem, requestedItemId)) {
                        itemIntroArea.setText(UiFormatters.readableText(detail.get("description"), ""));
                        itemImagesArea.setText(formatImagesForEdit(detail));
                        Document metadata = detail.get("metadata", Document.class);
                        itemMetadataArea.setText(metadata == null || metadata.isEmpty() ? "{}" : metadata.toJson());
                    }
                });
            }
        });

        queryItemsButton.addActionListener(event -> refreshItems.run());
        refreshItemsButton.addActionListener(event -> refreshItems.run());
        itemKeywordField.addActionListener(event -> refreshItems.run());
        resetItemsButton.addActionListener(event -> {
            itemKeywordField.setText("");
            itemCategoryBox.setSelectedIndex(0);
            refreshItems.run();
        });
        createItemButton.addActionListener(event -> showCreateItemDialog(refreshItems));
        basicItemButton.addActionListener(event -> runAdminTask("更新景点名称和分类", () -> businessService.updateItem(
                requireCurrentUserId(), requireSelectedItem(selectedItem).getItemId(), itemTitleField.getText(),
                requireCategoryId(itemEditCategoryBox)
        ), updated -> {
            setStatus(updated ? "景点名称和分类已更新" : "景点基本信息没有变化");
            refreshItems.run();
        }));
        pricingButton.addActionListener(event -> runAdminTask("更新票价和优惠", () -> businessService.updateItemPricing(
                requireCurrentUserId(),
                requireSelectedItem(selectedItem).getItemId(), parseRequiredAmount(itemPriceField.getText(), "票价"),
                parseRequiredAmount(itemDiscountField.getText(), "优惠减免比例")
        ), updated -> {
            setStatus(updated ? "景点票价和优惠已保存" : "票价和优惠没有变化");
            refreshItems.run();
        }));
        itemStatusButton.addActionListener(event -> runAdminTask("更新上下架状态", () -> businessService.updateItemStatus(
                requireCurrentUserId(),
                requireSelectedItem(selectedItem).getItemId(), itemStatusBox.getSelectedIndex()
        ), updated -> {
            setStatus(updated ? "景点上下架状态已更新" : "景点状态没有变化");
            refreshItems.run();
        }));
        updateIntroButton.addActionListener(event -> runAdminTask("更新景点详情", () -> businessService.updateItemDetail(
                requireCurrentUserId(),
                requireSelectedItem(selectedItem).getItemId(), itemIntroArea.getText(),
                parseImageLines(itemImagesArea.getText()), parseMetadataJson(itemMetadataArea.getText())
        ), updated -> setStatus(updated ? "景点简介、图片和扩展属性已更新" : "景点详情没有变化")));

        JPanel itemTop = new JPanel(new GridLayout(3, 1, 0, 8));
        itemTop.setOpaque(false);
        itemTop.add(wrapWithTitle("筛选景点", itemQueryToolbar));
        itemTop.add(wrapWithTitle("编辑所选景点", itemEditToolbar));
        itemTop.add(wrapWithTitle("编辑景点简介", introEditPanel));
        itemPage.add(itemTop, BorderLayout.NORTH);
        itemPage.add(wrapWithTitle("景点列表", new JScrollPane(itemTable)), BorderLayout.CENTER);

        JPanel categoryPage = new JPanel(new BorderLayout(12, 12));
        categoryPage.setOpaque(false);
        DefaultTableModel categoryModel = tableModel("分类ID", "分类名称", "上级分类", "层级路径");
        JTable categoryTable = createTable(categoryModel);
        categoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        List<Category> visibleCategories = new ArrayList<>();
        Category[] selectedCategory = new Category[1];
        JTextField categoryNameField = new JTextField(16);
        JComboBox<CategoryOption> parentCategoryBox = new JComboBox<>();
        parentCategoryBox.addItem(new CategoryOption("无上级分类", null));
        JButton createCategoryButton = primaryButton("新增分类");
        JButton updateCategoryButton = secondaryButton("更新所选分类");
        JButton refreshCategoryButton = secondaryButton("刷新");
        updateCategoryButton.setEnabled(false);
        JPanel categoryToolbar = toolbar();
        categoryToolbar.add(new JLabel("分类名称"));
        categoryToolbar.add(categoryNameField);
        categoryToolbar.add(new JLabel("上级分类"));
        categoryToolbar.add(parentCategoryBox);
        categoryToolbar.add(createCategoryButton);
        categoryToolbar.add(updateCategoryButton);
        categoryToolbar.add(refreshCategoryButton);

        Runnable refreshCategories = () -> runAdminTask("刷新分类", businessService::listCategories, categories -> {
            updateCategoryCache(categories);
            fillCategoryCombo(itemCategoryBox, categories, true, "全部类型");
            fillCategoryCombo(itemEditCategoryBox, categories, false, "");
            fillCategoryCombo(parentCategoryBox, categories, true, "无上级分类");
            categoryModel.setRowCount(0);
            visibleCategories.clear();
            visibleCategories.addAll(categories);
            Map<Long, String> categoryPaths = CategoryTreeFormatter.paths(categories);
            for (Category category : categories) {
                categoryModel.addRow(new Object[]{category.getCategoryId(), category.getName(),
                        category.getParentId() == null ? "-" : categoryName(category.getParentId()),
                        categoryPaths.get(category.getCategoryId())});
            }
            selectedCategory[0] = null;
            updateCategoryButton.setEnabled(false);
            setStatus("已加载 " + categories.size() + " 个分类");
            refreshItems.run();
        });
        categoryTable.getSelectionModel().addListSelectionListener(event -> {
            int row = categoryTable.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                Category category = visibleCategories.get(categoryTable.convertRowIndexToModel(row));
                selectedCategory[0] = category;
                categoryNameField.setText(category.getName());
                selectCategory(parentCategoryBox, category.getParentId());
                updateCategoryButton.setEnabled(true);
            }
        });
        refreshCategoryButton.addActionListener(event -> refreshCategories.run());
        createCategoryButton.addActionListener(event -> runAdminTask("新增分类", () -> businessService.createCategory(
                requireCurrentUserId(),
                categoryNameField.getText(), selectedCategoryId(parentCategoryBox)
        ), id -> {
            categoryNameField.setText("");
            setStatus("分类创建成功，编号：" + id);
            refreshCategories.run();
        }));
        updateCategoryButton.addActionListener(event -> runAdminTask("更新分类", () -> {
            if (selectedCategory[0] == null) {
                throw new IllegalArgumentException("请先从表格中选择分类");
            }
            return businessService.updateCategory(requireCurrentUserId(), selectedCategory[0].getCategoryId(),
                    categoryNameField.getText(), selectedCategoryId(parentCategoryBox));
        }, updated -> {
            setStatus(updated ? "分类名称和层级已更新" : "分类没有变化");
            refreshCategories.run();
        }));
        categoryNameField.addActionListener(event -> createCategoryButton.doClick());
        categoryPage.add(wrapWithTitle("新增或编辑分类", categoryToolbar), BorderLayout.NORTH);
        categoryPage.add(wrapWithTitle("分类列表", new JScrollPane(categoryTable)), BorderLayout.CENTER);

        manageTabs.addTab("景点管理", itemPage);
        manageTabs.addTab("分类管理", categoryPage);
        manageTabs.addTab("用户管理", createUserManagementPanel());
        manageTabs.addTab("票种与库存", createTicketInventoryManagementPanel());
        manageTabs.addTab("门票核销", createAdmissionManagementPanel());
        panel.add(manageTabs, BorderLayout.CENTER);
        refreshCategories.run();
        return panel;
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
            public AdminChangeResult changeStatus(long targetUserId, int status) {
                return adminUserService.changeUserStatus(actorUserId, targetUserId, status);
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

    private void loadCategoryChoices(JComboBox<CategoryOption> comboBox, boolean includeAll) {
        runTask("加载景点类型", businessService::listCategories, categories -> {
            updateCategoryCache(categories);
            fillCategoryCombo(comboBox, categories, includeAll, includeAll ? "全部类型" : "请选择类型");
        });
    }

    private void updateCategoryCache(List<Category> categories) {
        categoryNames.clear();
        for (Category category : categories) {
            categoryNames.put(category.getCategoryId(), category.getName());
        }
    }

    private void fillCategoryCombo(JComboBox<CategoryOption> comboBox, List<Category> categories,
                                   boolean includeEmpty, String emptyLabel) {
        UiCategoryOptions.fill(comboBox, categories, includeEmpty, emptyLabel);
    }

    private void showCreateItemDialog(Runnable refreshItems) {
        if (categoryNames.isEmpty()) {
            showError(new IllegalStateException("景点类型尚未加载，请先刷新分类"));
            return;
        }
        JTextField titleField = new JTextField(24);
        JComboBox<CategoryOption> categoryBox = new JComboBox<>();
        categoryNames.forEach((id, name) -> categoryBox.addItem(new CategoryOption(name, id)));
        JTextArea descriptionArea = new JTextArea(5, 24);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JTextField priceField = new JTextField("80.00", 10);
        JTextField discountField = new JTextField("0", 10);
        JTextArea imagesArea = new JTextArea(3, 24);
        JTextArea metadataArea = new JTextArea("{\"source\": \"Swing后台\"}", 3, 24);
        imagesArea.setLineWrap(true);
        metadataArea.setLineWrap(true);
        JPanel form = formPanel("新增景点");
        addField(form, 0, "景点名称", titleField);
        addField(form, 1, "景点类型", categoryBox);
        addTextAreaField(form, 2, "景点简介", descriptionArea);
        addField(form, 3, "固定票价", priceField);
        addField(form, 4, "优惠减免%", discountField);
        addTextAreaField(form, 5, "图片地址（每行一个）", imagesArea);
        addTextAreaField(form, 6, "扩展属性（JSON）", metadataArea);
        int result = JOptionPane.showConfirmDialog(this, form, "新增景点",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runAdminTask("新增景点", () -> businessService.createItem(requireCurrentUserId(), titleField.getText(), requireCategoryId(categoryBox),
                descriptionArea.getText(), parseImageLines(imagesArea.getText()), parseMetadataJson(metadataArea.getText()),
                parseRequiredAmount(priceField.getText(), "票价"), parseRequiredAmount(discountField.getText(), "优惠减免比例")), id -> {
            setStatus("景点创建成功，编号：" + id);
            refreshItems.run();
        });
    }

    private void showTicketAvailabilityDialog(Item item) {
        JTextField startDateField = new JTextField(LocalDate.now().plusDays(1).toString(), 12);
        JTextField endDateField = new JTextField(LocalDate.now().plusDays(14).toString(), 12);
        JPanel form = formPanel("查询“" + item.getTitle() + "”可售票种与日期");
        addField(form, 0, "开始日期", startDateField);
        addField(form, 1, "结束日期", endDateField);
        int result = JOptionPane.showConfirmDialog(this, form, "可售票种与日期",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runTask("查询可售票种与日期", () -> ticketInventoryService.listAvailable(requireCurrentUserId(), item.getItemId(),
                parseRequiredDate(startDateField.getText(), "开始日期"),
                parseRequiredDate(endDateField.getText(), "结束日期")), options -> {
            DefaultTableModel model = tableModel("票种ID", "票种名称", "游玩日期", "原价", "优惠", "折后价", "可售库存");
            for (var option : options) {
                model.addRow(new Object[]{option.ticketType().getTicketTypeId(), option.ticketType().getName(),
                        option.inventory().getVisitDate(), UiFormatters.money(option.ticketType().getOriginalPrice()),
                        discountText(option.ticketType().getDiscountRate()), UiFormatters.money(option.discountedPrice()),
                        option.inventory().getAvailableStock()});
            }
            JTable table = createTable(model);
            setColumnWidths(table, 75, 120, 110, 90, 80, 90, 90);
            JOptionPane.showMessageDialog(this, options.isEmpty()
                            ? new JLabel("所选日期范围暂无可售票种或库存")
                            : new JScrollPane(table),
                    "可售票种与日期", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void showPurchaseDialog(Item item, JTextArea detailArea) {
        if (item.getStatus() == null || item.getStatus() != 1) {
            showError(new IllegalArgumentException("该景点当前未上架，暂不能购买"));
            return;
        }
        runTask("加载可售票种", () -> ticketInventoryService.listAvailable(requireCurrentUserId(), item.getItemId(),
                LocalDate.now(), LocalDate.now().plusDays(30)), options -> {
            if (options.isEmpty()) {
                showError(new IllegalArgumentException("未来 30 天暂无可售票种或库存"));
                return;
            }
            showPendingOrderDialog(item, detailArea, options);
        });
    }

    private void showPendingOrderDialog(Item item, JTextArea detailArea, List<TicketAvailabilityDTO> options) {
        JComboBox<String> optionBox = new JComboBox<>();
        for (TicketAvailabilityDTO option : options) {
            optionBox.addItem(option.ticketType().getName() + " | " + option.inventory().getVisitDate()
                    + " | 折后 " + UiFormatters.money(option.discountedPrice())
                    + " | 可售 " + option.inventory().getAvailableStock());
        }
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        JComboBox<String> paymentBox = new JComboBox<>(new String[]{"微信", "支付宝", "银行卡"});
        JLabel amountLabel = new JLabel();
        amountLabel.setFont(SECTION_FONT);
        Runnable updateAmount = () -> {
            TicketAvailabilityDTO option = options.get(optionBox.getSelectedIndex());
            amountLabel.setText(UiFormatters.money(option.discountedPrice().multiply(
                    BigDecimal.valueOf((Integer) quantitySpinner.getValue()))));
        };
        quantitySpinner.addChangeListener(event -> updateAmount.run());
        optionBox.addActionListener(event -> updateAmount.run());
        updateAmount.run();
        JPanel form = formPanel("创建待支付订单");
        addField(form, 0, "景点", new JLabel(item.getTitle()));
        addField(form, 1, "票种与日期", optionBox);
        addField(form, 2, "购买票数", quantitySpinner);
        addField(form, 3, "付款方式", paymentBox);
        addField(form, 4, "待支付金额", amountLabel);
        addField(form, 5, "支付提示", new JLabel("创建后请到“我的订单”主动确认支付，15 分钟过期"));
        int result = JOptionPane.showConfirmDialog(this, form, "创建待支付订单",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        TicketAvailabilityDTO selected = options.get(optionBox.getSelectedIndex());
        runTask("创建待支付订单", () -> orderLifecycleService.createPendingOrder(requireCurrentUserId(),
                selected.ticketType().getTicketTypeId(), selected.inventory().getVisitDate(),
                (Integer) quantitySpinner.getValue(), (String) paymentBox.getSelectedItem(), "127.0.0.1"), action ->
                detailArea.setText(action.message() + System.lineSeparator()
                        + "景点：" + item.getTitle() + System.lineSeparator()
                        + "票种：" + selected.ticketType().getName() + System.lineSeparator()
                        + "游玩日期：" + selected.inventory().getVisitDate() + System.lineSeparator()
                        + "订单号：" + action.orderId() + System.lineSeparator()
                        + "待支付金额：" + amountLabel.getText() + System.lineSeparator()
                        + "请进入“我的订单”确认支付。"));
    }

    private void showCommentDialog(Item item, JTextArea detailArea) {
        JSpinner ratingSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        JTextArea commentArea = new JTextArea(5, 28);
        JTextField tagsField = new JTextField(28);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        JPanel form = formPanel("发表评论");
        addField(form, 0, "景点", new JLabel(item.getTitle()));
        addField(form, 1, "评分", ratingSpinner);
        addTextAreaField(form, 2, "评论内容", commentArea);
        addField(form, 3, "标签（逗号分隔）", tagsField);
        int result = JOptionPane.showConfirmDialog(this, form, "发表评论",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runTask("发表评论", () -> commentService.submit(requireCurrentUserId(), item.getItemId(),
                commentArea.getText(), (Integer) ratingSpinner.getValue(), parseTags(tagsField.getText()),
                "127.0.0.1"), resultMessage -> {
            setStatus(resultMessage.message());
            if (!resultMessage.auditRecorded()) {
                JOptionPane.showMessageDialog(this, resultMessage.message(),
                        "评论已保存（审计警告）", JOptionPane.WARNING_MESSAGE);
            }
            runTask("刷新游客评论", () -> commentService.listForItem(
                    requireCurrentUserId(), item.getItemId(), 20),
                    dto -> {
                        detailArea.setText(formatCommentViews(item.getTitle(), dto));
                        setStatus(resultMessage.message());
                    });
        });
    }

    private Item requireSelectedItem(Item[] selectedItem) {
        if (selectedItem == null || selectedItem.length == 0 || selectedItem[0] == null) {
            throw new IllegalArgumentException("请先从表格中选择一个景点");
        }
        return selectedItem[0];
    }

    private boolean isSelectedItem(Item[] selectedItem, long itemId) {
        return selectedItem != null && selectedItem.length > 0 && selectedItem[0] != null
                && selectedItem[0].getItemId() != null && selectedItem[0].getItemId() == itemId;
    }

    private void resetItemSelection(JTable table, Item[] selectedItem, JButton... actionButtons) {
        table.clearSelection();
        selectedItem[0] = null;
        for (JButton button : actionButtons) {
            button.setEnabled(false);
        }
    }

    private JPanel pagePanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.setBackground(BACKGROUND);
        return panel;
    }

    private JScrollPane scrollPage(Component content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        return scrollPane;
    }

    private JPanel formPanel(String title) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        JLabel heading = new JLabel(title);
        heading.setFont(SECTION_FONT);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        outer.add(heading, BorderLayout.NORTH);
        outer.add(fields, BorderLayout.CENTER);
        return outer;
    }

    private JPanel wrapWithTitle(String title, Component content) {
        return UiComponents.card(title, content);
    }

    private JButton primaryButton(String text) {
        return UiComponents.primaryButton(text);
    }

    private JButton secondaryButton(String text) {
        return UiComponents.secondaryButton(text);
    }

    private JPanel toolbar() {
        return UiComponents.toolbar();
    }

    private JTextArea createTextArea(int rows, int columns) {
        return UiComponents.readOnlyTextArea(rows, columns);
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = UiComponents.table(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        return table;
    }

    private void setColumnWidths(JTable table, int... widths) {
        int columnCount = Math.min(table.getColumnCount(), widths.length);
        for (int i = 0; i < columnCount; i += 1) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private Long selectedCategoryId(JComboBox<CategoryOption> categoryBox) {
        CategoryOption choice = (CategoryOption) categoryBox.getSelectedItem();
        return choice == null ? null : choice.categoryId();
    }

    private void selectCategory(JComboBox<CategoryOption> categoryBox, Long categoryId) {
        for (int index = 0; index < categoryBox.getItemCount(); index += 1) {
            CategoryOption option = categoryBox.getItemAt(index);
            if (java.util.Objects.equals(option.categoryId(), categoryId)) {
                categoryBox.setSelectedIndex(index);
                return;
            }
        }
        categoryBox.setSelectedIndex(categoryBox.getItemCount() == 0 ? -1 : 0);
    }

    private List<String> parseImageLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return text.lines().map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
    }

    private List<String> parseTags(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("[,，]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private Document parseMetadataJson(String text) {
        if (text == null || text.isBlank()) {
            return new Document();
        }
        try {
            return Document.parse(text.trim());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("扩展属性必须是合法 JSON 对象", exception);
        }
    }

    private String formatImagesForEdit(Document detail) {
        if (detail == null) {
            return "";
        }
        List<?> images = detail.getList("images", Object.class);
        if (images == null || images.isEmpty()) {
            return "";
        }
        return images.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(System.lineSeparator()));
    }

    private long requireCategoryId(JComboBox<CategoryOption> categoryBox) {
        Long categoryId = selectedCategoryId(categoryBox);
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("请选择景点类型");
        }
        return categoryId;
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return "-";
        }
        return categoryNames.getOrDefault(categoryId, "未分类");
    }

    private String discountText(BigDecimal discountRate) {
        return UiFormatters.discount(discountRate);
    }

    private DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void addField(JPanel formPanel, int row, String label, Component field) {
        JPanel fields = (JPanel) formPanel.getComponent(1);
        GridBagConstraints labelConstraints = formConstraints(row, 0);
        fields.add(new JLabel(label), labelConstraints);
        GridBagConstraints fieldConstraints = formConstraints(row, 1);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        fields.add(field, fieldConstraints);
    }

    private void addTextAreaField(JPanel formPanel, int row, String label, JTextArea textArea) {
        addField(formPanel, row, label, new JScrollPane(textArea));
    }

    private void addFormButton(JPanel formPanel, int row, JButton button) {
        JPanel fields = (JPanel) formPanel.getComponent(1);
        GridBagConstraints gbc = formConstraints(row, 1);
        gbc.fill = GridBagConstraints.NONE;
        fields.add(button, gbc);
    }

    private void addFormButtons(JPanel formPanel, int row, JButton... buttons) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.setOpaque(false);
        for (JButton button : buttons) {
            buttonPanel.add(button);
        }
        JPanel fields = (JPanel) formPanel.getComponent(1);
        GridBagConstraints gbc = formConstraints(row, 1);
        gbc.fill = GridBagConstraints.NONE;
        fields.add(buttonPanel, gbc);
    }

    private void addFormMessage(JPanel formPanel, int row, JLabel message) {
        JPanel fields = (JPanel) formPanel.getComponent(1);
        GridBagConstraints gbc = formConstraints(row, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        fields.add(message, gbc);
    }

    private GridBagConstraints formConstraints(int row, int column) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = column;
        gbc.gridy = row;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
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

    private <T> void runTask(String name, Callable<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
        taskRunner.run(name, task, onSuccess, onError);
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

    private void setTextKeepingScroll(JTextArea textArea, String text) {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, textArea);
        if (scrollPane == null) {
            textArea.setText(text);
            return;
        }
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
        int verticalValue = verticalBar.getValue();
        int horizontalValue = horizontalBar.getValue();
        textArea.setText(text);
        SwingUtilities.invokeLater(() -> {
            verticalBar.setValue(Math.min(verticalValue, verticalBar.getMaximum()));
            horizontalBar.setValue(Math.min(horizontalValue, horizontalBar.getMaximum()));
        });
    }

    private void clearTextFields(Component component) {
        if (component instanceof JTextField textField) {
            textField.setText("");
            return;
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                clearTextFields(child);
            }
        }
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

    private String formatItemIntroduction(CrossDatabaseItemDTO dto) {
        StringBuilder builder = new StringBuilder();
        Item item = dto.getItem();
        builder.append("景点简介").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append(item.getTitle()).append(System.lineSeparator());
        builder.append("类型：").append(categoryName(item.getCategoryId())).append(System.lineSeparator());
        builder.append("门票原价：").append(UiFormatters.money(item.getPrice())).append(System.lineSeparator());
        builder.append("优惠：").append(discountText(item.getDiscountRate())).append(System.lineSeparator());
        builder.append("折后单价：").append(UiFormatters.money(UiFormatters.discountedUnitPrice(
                item.getPrice(), item.getDiscountRate()))).append(System.lineSeparator());
        builder.append("状态：").append(formatItemStatus(item.getStatus())).append(System.lineSeparator())
                .append(System.lineSeparator());
        Document detail = dto.getDetail();
        builder.append("简介：").append(formatItemDetailDocument(detail));
        if (detail != null) {
            List<?> images = detail.getList("images", Object.class);
            Document metadata = detail.get("metadata", Document.class);
            builder.append(System.lineSeparator()).append(System.lineSeparator())
                    .append("图片地址：").append(images == null || images.isEmpty() ? "暂无" : images)
                    .append(System.lineSeparator())
                    .append("扩展属性：").append(metadata == null || metadata.isEmpty() ? "暂无" : metadata.toJson());
        }
        return builder.toString();
    }

    private String formatItemCommentsView(CrossDatabaseItemDTO dto) {
        StringBuilder builder = new StringBuilder();
        builder.append("游客评论").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("景点：").append(dto.getItem().getTitle()).append(System.lineSeparator());
        builder.append("评分概览：").append(formatRatingSummary(dto.getRatingSummary()))
                .append(System.lineSeparator());
        builder.append("评论列表：").append(System.lineSeparator())
                .append(formatComments(dto.getComments()));
        return builder.toString();
    }

    private String formatCommentViews(String itemTitle, CommentListDTO dto) {
        StringBuilder builder = new StringBuilder("游客评论")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("景点：").append(itemTitle).append(System.lineSeparator())
                .append("评分概览：").append(formatRatingSummary(dto.ratingSummary()))
                .append(System.lineSeparator()).append(System.lineSeparator());
        if (dto.comments().isEmpty()) {
            return builder.append("暂无评论").toString();
        }
        int index = 1;
        for (var comment : dto.comments()) {
            builder.append(index++).append(". 用户：").append(comment.displayUsername())
                    .append("  评分：").append(comment.rating()).append(System.lineSeparator())
                    .append("   正文：").append(valueText(comment.content())).append(System.lineSeparator())
                    .append("   标签：").append(comment.tags().isEmpty() ? "无" : String.join("、", comment.tags()))
                    .append(System.lineSeparator())
                    .append("   创建：").append(formatDate(comment.createdAt()))
                    .append("  更新：").append(formatDate(comment.updatedAt()))
                    .append(System.lineSeparator()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String formatItemDetailDocument(Document detail) {
        if (detail == null || detail.isEmpty()) {
            return "管理员暂未填写景点简介";
        }
        return UiFormatters.readableText(detail.get("description"), "管理员暂未填写景点简介");
    }

    private String formatRatingSummary(Document document) {
        if (document == null || document.isEmpty()) {
            return "暂无评分";
        }
        return "评论数：" + numberText(document.get("comment_count"))
                + "，平均分：" + decimalText(document.get("avg_rating"))
                + "，最高分：" + numberText(document.get("max_rating"))
                + "，最低分：" + numberText(document.get("min_rating"));
    }

    private String formatComments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无评论";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            builder.append(index).append(". 评分：").append(numberText(document.get("rating")))
                    .append("  时间：").append(formatDate(document.get("created_at")))
                    .append(System.lineSeparator())
                    .append("   内容：").append(UiFormatters.readableText(document.get("content"), "该评论没有文字内容"))
                    .append(System.lineSeparator());
            index += 1;
        }
        return builder.toString();
    }

    private String formatDate(Object value) {
        return UiFormatters.date(value);
    }

    private String valueText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String fieldText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String numberText(Object value) {
        return value instanceof Number number ? String.valueOf(number.longValue()) : valueText(value);
    }

    private String decimalText(Object value) {
        if (value instanceof Number number) {
            return String.format("%.2f", number.doubleValue());
        }
        return valueText(value);
    }

    private String formatItemStatus(Integer status) {
        return UiFormatters.itemStatus(status);
    }

    private String formatOrderStatus(Integer status) {
        return UiFormatters.orderStatus(status);
    }

    private long requireCurrentUserId() {
        if (currentUser == null || currentUser.getUserId() == null || currentUser.getUserId() <= 0) {
            throw new IllegalStateException("请先登录或填写用户ID");
        }
        return currentUser.getUserId();
    }

    private Long parseOptionalLong(String value) {
        return UiInputParsers.optionalLong(value);
    }

    private long parseRequiredLong(String value, String fieldName) {
        return UiInputParsers.requiredLong(value, fieldName);
    }

    private int parseRequiredInt(String value, String fieldName) {
        return UiInputParsers.requiredInt(value, fieldName);
    }

    private int parseOptionalInt(String value, int defaultValue, String fieldName) {
        return UiInputParsers.optionalInt(value, defaultValue, fieldName);
    }

    private BigDecimal parseRequiredAmount(String value, String fieldName) {
        return UiInputParsers.requiredAmount(value, fieldName);
    }

    private LocalDate parseRequiredDate(String value, String fieldName) {
        return UiInputParsers.requiredDate(value, fieldName);
    }

    private String blankToNull(String value) {
        return UiInputParsers.blankToNull(value);
    }

}
