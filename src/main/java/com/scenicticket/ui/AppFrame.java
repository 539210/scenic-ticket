package com.scenicticket.ui;

import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.dto.AuditLogQuery;
import com.scenicticket.dto.HotItemRankingDTO;
import com.scenicticket.dto.LoginResult;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.dto.StatisticsReportDTO;
import com.scenicticket.dto.AdminUserDetailDTO;
import com.scenicticket.dto.UserSearchCriteria;
import com.scenicticket.dto.TicketAvailabilityDTO;
import com.scenicticket.dto.CommentListDTO;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import com.scenicticket.model.Order;
import com.scenicticket.model.Profile;
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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
import java.time.ZoneId;
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
    private static final String ALL_OPTION = "全部";
    private static final String[] KEYWORD_OPTIONS = {
            ALL_OPTION, "南山", "云岭", "青河", "古城", "海湾", "星湖", "观景", "博物馆", "亲子", "水上", "森林"
    };

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
    private final JTextArea homeSummaryArea = createTextArea(10, 80);
    private final JLabel homeUserValue = new JLabel("未登录");
    private final JLabel homeRoleValue = new JLabel("-");
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
        refreshHomeSummary();
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
        JPanel panel = pagePanel(new BorderLayout(12, 12));

        JPanel metrics = new JPanel(new GridLayout(1, 2, 12, 12));
        metrics.setOpaque(false);
        metrics.add(metricCard("当前账号", homeUserValue));
        metrics.add(metricCard("账号类型", homeRoleValue));

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickActions.setOpaque(false);
        quickActions.add(navButton("完善档案", "个人档案"));
        quickActions.add(navButton("浏览景点", "景点浏览"));
        quickActions.add(navButton("我的订单", "我的订单"));
        quickActions.add(navButton("查看报表", "统计报表"));
        JButton refreshButton = secondaryButton("刷新");
        refreshButton.addActionListener(event -> {
            refreshHomeSummary();
            setStatus("首页已刷新");
        });
        quickActions.add(refreshButton);
        if (isCurrentAdmin()) {
            quickActions.add(navButton("后台管理", "后台管理"));
            quickActions.add(navButton("系统审计", "系统审计"));
        }

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(metrics, BorderLayout.CENTER);
        top.add(quickActions, BorderLayout.SOUTH);

        homeSummaryArea.setText("");
        panel.add(top, BorderLayout.NORTH);
        panel.add(wrapWithTitle("欢迎使用", homeSummaryArea), BorderLayout.CENTER);
        return panel;
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
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField userIdField = new JTextField(12);
        JTextField realNameField = new JTextField(24);
        JTextField idCardField = new JTextField(24);
        JTextField addressField = new JTextField(32);
        JTextArea notesArea = new JTextArea(5, 32);
        JButton refreshButton = secondaryButton("刷新档案");
        JButton saveButton = primaryButton("保存档案");
        JLabel message = new JLabel(" ");

        JPanel form = formPanel("个人档案");
        userIdField.setText(String.valueOf(requireCurrentUserId()));
        addField(form, 0, "真实姓名", realNameField);
        addField(form, 1, "证件号", idCardField);
        addField(form, 2, "联系地址", addressField);
        addTextAreaField(form, 3, "个人简介", notesArea);
        addFormButtons(form, 4, refreshButton, saveButton);
        addFormMessage(form, 5, message);

        refreshButton.addActionListener(event -> runTask("刷新档案", () -> userService.getProfile(
                requireCurrentUserId(),
                requireCurrentUserId()
        ), profile -> {
            if (profile.isPresent()) {
                fillProfileForm(profile.get(), userIdField, realNameField, idCardField, addressField, notesArea);
                message.setText("档案已刷新");
            } else {
                realNameField.setText("");
                idCardField.setText("");
                addressField.setText("");
                notesArea.setText("");
                message.setText("暂无档案信息，可以填写后保存");
            }
        }));

        saveButton.addActionListener(event -> runTask("保存档案", () -> {
            Profile profile = new Profile();
            profile.setUserId(requireCurrentUserId());
            profile.setRealName(realNameField.getText());
            profile.setIdCard(idCardField.getText());
            profile.setAddress(addressField.getText());
            profile.setNotes(notesArea.getText());
            return userService.updateProfile(requireCurrentUserId(), profile);
        }, saved -> message.setText(saved ? "档案已保存" : "档案未更新")));

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createItemPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JComboBox<String> keywordBox = new JComboBox<>(KEYWORD_OPTIONS);
        JTextField keywordField = new JTextField(14);
        JComboBox<CategoryOption> categoryBox = new JComboBox<>();
        categoryBox.addItem(new CategoryOption("全部类型", null));
        JTextArea overviewArea = createTextArea(18, 34);
        JTextArea introductionArea = createTextArea(18, 34);
        JTextArea commentsArea = createTextArea(18, 34);
        DefaultTableModel tableModel = tableModel("景点名称", "类型", "原价", "优惠", "折后价", "推荐分", "状态");
        JTable table = createTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setColumnWidths(table, 210, 110, 85, 90, 90, 80, 75);
        overviewArea.setText("请选择查询条件，或点击“查询”浏览全部景点。");
        introductionArea.setText("请选择景点后点击“景点简介”。");
        commentsArea.setText("请选择景点后点击“游客评论”。");
        List<Item> visibleItems = new ArrayList<>();
        Map<Long, String> recommendationReasons = new LinkedHashMap<>();
        Item[] selectedItem = new Item[1];

        JPanel searchToolbar = toolbar();
        JButton searchButton = primaryButton("查询");
        JButton recommendButton = secondaryButton("推荐");
        JPopupMenu recommendMenu = new JPopupMenu();
        JMenuItem personalRecommendItem = new JMenuItem("为你推荐");
        JMenuItem hotRecommendItem = new JMenuItem("热门");
        JMenuItem ratedRecommendItem = new JMenuItem("高分");
        recommendMenu.add(personalRecommendItem);
        recommendMenu.add(hotRecommendItem);
        recommendMenu.add(ratedRecommendItem);
        JButton clearButton = secondaryButton("重置");
        searchToolbar.add(new JLabel("关键词"));
        searchToolbar.add(keywordBox);
        searchToolbar.add(new JLabel("自定义"));
        searchToolbar.add(keywordField);
        searchToolbar.add(new JLabel("景点类型"));
        searchToolbar.add(categoryBox);
        searchToolbar.add(searchButton);
        searchToolbar.add(recommendButton);
        searchToolbar.add(clearButton);

        JButton detailButton = secondaryButton("景点简介");
        JButton commentsButton = secondaryButton("游客评论");
        JButton availabilityButton = secondaryButton("可售票种与日期");
        JButton orderButton = primaryButton("购买门票");
        JButton commentButton = secondaryButton("发表评论");
        detailButton.setEnabled(false);
        commentsButton.setEnabled(false);
        availabilityButton.setEnabled(false);
        orderButton.setEnabled(false);
        commentButton.setEnabled(false);
        JPanel detailActions = new JPanel(new GridLayout(3, 2, 10, 10));
        detailActions.setOpaque(false);
        detailActions.add(detailButton);
        detailActions.add(commentsButton);
        detailActions.add(availabilityButton);
        detailActions.add(orderButton);
        detailActions.add(commentButton);
        detailActions.add(new JLabel(""));
        JTabbedPane scenicInfoTabs = new JTabbedPane();
        scenicInfoTabs.addTab("景点概览", new JScrollPane(overviewArea));
        scenicInfoTabs.addTab("景点简介", new JScrollPane(introductionArea));
        scenicInfoTabs.addTab("游客评论", new JScrollPane(commentsArea));
        JPanel detailPanel = new JPanel(new BorderLayout(0, 10));
        detailPanel.setOpaque(false);
        detailPanel.add(scenicInfoTabs, BorderLayout.CENTER);
        detailPanel.add(detailActions, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(event -> {
            int viewRow = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && viewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                selectedItem[0] = visibleItems.get(modelRow);
                Item item = selectedItem[0];
                String reason = recommendationReasons.get(item.getItemId());
                overviewArea.setText(item.getTitle() + System.lineSeparator()
                        + "类型：" + categoryName(item.getCategoryId()) + System.lineSeparator()
                        + "原价：" + UiFormatters.money(item.getPrice()) + "    优惠："
                        + discountText(item.getDiscountRate()) + System.lineSeparator()
                        + "折后价：" + UiFormatters.money(UiFormatters.discountedUnitPrice(
                        item.getPrice(), item.getDiscountRate())) + System.lineSeparator()
                        + "状态：" + formatItemStatus(item.getStatus())
                        + (reason == null ? "" : System.lineSeparator() + "推荐理由：" + reason)
                        + System.lineSeparator() + System.lineSeparator()
                        + "点击“景点简介”查看景区介绍，点击“游客评论”查看评价。" );
                introductionArea.setText("尚未加载“" + item.getTitle() + "”的景点简介。");
                commentsArea.setText("尚未加载“" + item.getTitle() + "”的游客评论。");
                scenicInfoTabs.setSelectedIndex(0);
                detailButton.setEnabled(true);
                commentsButton.setEnabled(true);
                availabilityButton.setEnabled(item.getStatus() != null && item.getStatus() == 1);
                orderButton.setEnabled(item.getStatus() != null && item.getStatus() == 1);
                commentButton.setEnabled(true);
            }
        });

        Consumer<List<Item>> fillItems = items -> {
            tableModel.setRowCount(0);
            visibleItems.clear();
            recommendationReasons.clear();
            for (Item item : items) {
                visibleItems.add(item);
                tableModel.addRow(new Object[]{
                        item.getTitle(), categoryName(item.getCategoryId()), UiFormatters.money(item.getPrice()),
                        discountText(item.getDiscountRate()), UiFormatters.money(UiFormatters.discountedUnitPrice(
                        item.getPrice(), item.getDiscountRate())), "-", formatItemStatus(item.getStatus())
                });
            }
            resetItemSelection(table, selectedItem, detailButton, commentsButton, availabilityButton, orderButton, commentButton);
            overviewArea.setText(items.isEmpty() ? "没有找到符合条件的景点，请调整查询条件。" : "共找到 " + items.size() + " 个景点，请从左侧列表选择。" );
            introductionArea.setText("请选择景点后点击“景点简介”。");
            commentsArea.setText("请选择景点后点击“游客评论”。");
            setStatus("查询到 " + items.size() + " 个景点");
        };

        Consumer<List<RecommendationDTO>> fillRecommendations = recommendations -> {
            tableModel.setRowCount(0);
            visibleItems.clear();
            recommendationReasons.clear();
            for (RecommendationDTO recommendation : recommendations) {
                Item item = recommendation.getItem();
                if (item == null) {
                    continue;
                }
                visibleItems.add(item);
                recommendationReasons.put(item.getItemId(), recommendation.getReason());
                tableModel.addRow(new Object[]{
                        item.getTitle(), categoryName(item.getCategoryId()), UiFormatters.money(item.getPrice()),
                        discountText(item.getDiscountRate()), UiFormatters.money(UiFormatters.discountedUnitPrice(
                        item.getPrice(), item.getDiscountRate())), formatRecommendationScore(recommendation.getScore()),
                        formatItemStatus(item.getStatus())
                });
            }
            resetItemSelection(table, selectedItem, detailButton, commentsButton, availabilityButton, orderButton, commentButton);
            overviewArea.setText(recommendations.isEmpty() ? "暂时没有推荐结果。" : "已生成 " + recommendations.size() + " 个推荐结果，请选择景点查看推荐理由。" );
            introductionArea.setText("请选择景点后点击“景点简介”。");
            commentsArea.setText("请选择景点后点击“游客评论”。");
            setStatus("已生成 " + recommendations.size() + " 个推荐景点");
        };

        searchButton.addActionListener(event -> runTask("景点查询", () -> businessService.searchItems(
                buildSearchKeyword((String) keywordBox.getSelectedItem(), keywordField.getText()),
                selectedCategoryId(categoryBox), 50, 0
        ), fillItems));

        recommendButton.addActionListener(event -> recommendMenu.show(recommendButton, 0, recommendButton.getHeight()));

        personalRecommendItem.addActionListener(event -> runTask("为你推荐", () -> recommendService.recommendForUser(
                requireCurrentUserId(), 10
        ), fillRecommendations));

        ratedRecommendItem.addActionListener(event -> runTask("高分推荐", () -> recommendService.recommendTopRatedItems(10),
                fillRecommendations));

        hotRecommendItem.addActionListener(event -> runTask("热门推荐", () -> recommendService.recommendHotItems(null, null, 10),
                fillRecommendations));

        clearButton.addActionListener(event -> {
            keywordBox.setSelectedItem(ALL_OPTION);
            keywordField.setText("");
            categoryBox.setSelectedIndex(0);
            runTask("重置景点列表", () -> businessService.searchItems(null, null, 50, 0), fillItems);
        });

        detailButton.addActionListener(event -> {
            long requestedItemId = requireSelectedItem(selectedItem).getItemId();
            runTask("景点详情", () -> crossDatabaseQueryService.getItemDetail(requestedItemId, 8), dto -> {
                if (isSelectedItem(selectedItem, requestedItemId)) {
                    introductionArea.setText(formatItemIntroduction(dto));
                    scenicInfoTabs.setSelectedIndex(1);
                }
            });
        });

        commentsButton.addActionListener(event -> {
            Item requestedItem = requireSelectedItem(selectedItem);
            long requestedItemId = requestedItem.getItemId();
            runTask("游客评论", () -> commentService.listForItem(requireCurrentUserId(), requestedItemId, 20), dto -> {
                if (isSelectedItem(selectedItem, requestedItemId)) {
                    commentsArea.setText(formatCommentViews(requestedItem.getTitle(), dto));
                    scenicInfoTabs.setSelectedIndex(2);
                }
            });
        });

        availabilityButton.addActionListener(event -> showTicketAvailabilityDialog(requireSelectedItem(selectedItem)));

        orderButton.addActionListener(event -> showPurchaseDialog(requireSelectedItem(selectedItem), overviewArea));
        commentButton.addActionListener(event -> {
            Item item = requireSelectedItem(selectedItem);
            showCommentDialog(item, commentsArea);
        });

        keywordField.addActionListener(event -> searchButton.doClick());
        loadCategoryChoices(categoryBox, true);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrapWithTitle("景点列表", new JScrollPane(table)),
                wrapWithTitle("景点信息", detailPanel));
        splitPane.setResizeWeight(0.64);
        splitPane.setDividerLocation(760);
        panel.add(wrapWithTitle("查询景点", searchToolbar), BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOrderPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField userIdField = new JTextField(10);
        JTextField queryOrderIdField = new JTextField(10);
        JComboBox<String> queryStatusBox = new JComboBox<>(new String[]{"全部状态", "0-待支付", "1-已支付", "2-已取消", "3-已完成"});
        boolean adminOrderView = isCurrentAdmin();
        DefaultTableModel model = OrderTableModels.create(adminOrderView);
        JTable table = createTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setColumnWidths(table, OrderTableModels.columnWidths(adminOrderView));
        Long[] selectedOrderId = new Long[1];
        List<OrderViewDTO> visibleOrderViews = new ArrayList<>();

        JPanel queryToolbar = toolbar();
        JButton listButton = primaryButton("查询订单");
        JButton refreshButton = secondaryButton("刷新");
        JButton resetButton = secondaryButton("重置");
        JButton payButton = primaryButton("确认支付");
        JButton cancelButton = secondaryButton("取消待支付订单");
        JButton refundButton = secondaryButton("申请模拟退款");
        payButton.setEnabled(false);
        cancelButton.setEnabled(false);
        refundButton.setEnabled(false);
        if (isCurrentAdmin()) {
            queryToolbar.add(new JLabel("用户ID"));
            queryToolbar.add(userIdField);
        }
        queryToolbar.add(new JLabel("订单号"));
        queryToolbar.add(queryOrderIdField);
        queryToolbar.add(new JLabel("状态"));
        queryToolbar.add(queryStatusBox);
        queryToolbar.add(listButton);
        queryToolbar.add(refreshButton);
        queryToolbar.add(resetButton);

        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 8));
        controls.setOpaque(false);
        controls.add(wrapWithTitle("订单查询", queryToolbar));
        JPanel actionToolbar = toolbar();
        JLabel selectedOrderLabel = new JLabel("请先从表格选择订单");
        actionToolbar.add(selectedOrderLabel);
        actionToolbar.add(payButton);
        actionToolbar.add(cancelButton);
        actionToolbar.add(refundButton);
        controls.add(wrapWithTitle("订单生命周期操作", actionToolbar));
        table.getSelectionModel().addListSelectionListener(event -> {
            int row = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                Order selectedOrder = visibleOrderViews.get(modelRow).getOrder();
                selectedOrderId[0] = selectedOrder.getOrderId();
                selectedOrderLabel.setText("已选择订单：" + selectedOrderId[0]);
                OrderActionPolicy.Availability availability = OrderActionPolicy.evaluate(
                        requireCurrentUserId(), selectedOrder, LocalDate.now());
                payButton.setEnabled(availability.canPay());
                cancelButton.setEnabled(availability.canCancel());
                refundButton.setEnabled(availability.canRefund());
            }
        });

        Runnable refreshOrders = () -> runTask("订单查询", () -> {
            orderLifecycleService.expireDueOrders(100);
            Long queryUserId = isCurrentAdmin()
                    ? parseOptionalLong(userIdField.getText())
                    : Long.valueOf(requireCurrentUserId());
            return businessService.searchOrderViews(
                    requireCurrentUserId(),
                    queryUserId,
                    parseOptionalLong(queryOrderIdField.getText()),
                    selectedOrderStatus(queryStatusBox),
                    50,
                    0
            );
        }, orderViews -> {
            visibleOrderViews.clear();
            visibleOrderViews.addAll(orderViews);
            OrderTableModels.fill(model, orderViews, adminOrderView);
            selectedOrderId[0] = null;
            payButton.setEnabled(false);
            cancelButton.setEnabled(false);
            refundButton.setEnabled(false);
            selectedOrderLabel.setText("请先从表格选择订单");
            setStatus("查询到 " + orderViews.size() + " 条订单");
        });
        listButton.addActionListener(event -> refreshOrders.run());
        refreshButton.addActionListener(event -> refreshOrders.run());
        resetButton.addActionListener(event -> {
            userIdField.setText("");
            queryOrderIdField.setText("");
            queryStatusBox.setSelectedIndex(0);
            refreshOrders.run();
        });
        queryOrderIdField.addActionListener(event -> refreshOrders.run());

        payButton.addActionListener(event -> runTask("确认支付", () -> orderLifecycleService.pay(
                requireCurrentUserId(), requireSelectedOrderId(selectedOrderId), "127.0.0.1"), action -> {
            setStatus(action.message());
            refreshOrders.run();
        }));
        cancelButton.addActionListener(event -> runTask("取消待支付订单", () -> orderLifecycleService.cancelPending(
                requireCurrentUserId(), requireSelectedOrderId(selectedOrderId), "127.0.0.1"), action -> {
            setStatus(action.message());
            refreshOrders.run();
        }));
        refundButton.addActionListener(event -> {
            String reason = JOptionPane.showInputDialog(this, "请输入退款原因", "申请模拟退款", JOptionPane.PLAIN_MESSAGE);
            if (reason == null) {
                return;
            }
            runTask("模拟退款", () -> orderLifecycleService.refund(requireCurrentUserId(),
                    requireSelectedOrderId(selectedOrderId), reason, "127.0.0.1"), action -> {
                setStatus(action.message());
                refreshOrders.run();
            });
        });

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
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
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        JTextField usernameField = new JTextField(12);
        JTextField emailField = new JTextField(16);
        JComboBox<String> roleFilter = new JComboBox<>(new String[]{"全部角色", "管理员", "普通用户"});
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"全部状态", "启用", "禁用"});
        JButton queryButton = primaryButton("查询用户");
        JButton resetButton = secondaryButton("重置");

        JPanel filters = toolbar();
        filters.add(new JLabel("用户名"));
        filters.add(usernameField);
        filters.add(new JLabel("邮箱"));
        filters.add(emailField);
        filters.add(new JLabel("角色"));
        filters.add(roleFilter);
        filters.add(new JLabel("状态"));
        filters.add(statusFilter);
        filters.add(queryButton);
        filters.add(resetButton);

        DefaultTableModel model = tableModel("用户ID", "用户名", "邮箱", "手机号", "角色", "状态", "创建时间");
        JTable table = createTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setColumnWidths(table, 70, 120, 190, 120, 90, 70, 170);
        List<User> visibleUsers = new ArrayList<>();
        User[] selectedUser = new User[1];

        JTextArea detailArea = createTextArea(14, 38);
        detailArea.setText("请先查询并选择用户。\n服务层会再次校验管理员权限。");
        JComboBox<String> targetStatusBox = new JComboBox<>(new String[]{"禁用", "启用"});
        JComboBox<String> targetRoleBox = new JComboBox<>(new String[]{"普通用户", "管理员"});
        JButton statusButton = primaryButton("更新状态");
        JButton roleButton = secondaryButton("更新角色");
        statusButton.setEnabled(false);
        roleButton.setEnabled(false);
        JPanel actions = toolbar();
        actions.add(new JLabel("状态"));
        actions.add(targetStatusBox);
        actions.add(statusButton);
        actions.add(new JLabel("角色"));
        actions.add(targetRoleBox);
        actions.add(roleButton);

        JPanel detailPanel = new JPanel(new BorderLayout(0, 10));
        detailPanel.setOpaque(false);
        detailPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        detailPanel.add(actions, BorderLayout.SOUTH);

        Runnable refreshUsers = () -> runAdminTask("查询用户", () -> adminUserService.searchUsers(
                requireCurrentUserId(), new UserSearchCriteria(
                        blankToNull(usernameField.getText()), blankToNull(emailField.getText()),
                        selectedUserRoleFilter(roleFilter), selectedUserStatusFilter(statusFilter), 100, 0)
        ), users -> {
            visibleUsers.clear();
            visibleUsers.addAll(users);
            model.setRowCount(0);
            for (User user : users) {
                model.addRow(new Object[]{user.getUserId(), user.getUsername(), user.getEmail(), user.getPhone(),
                        roleDisplay(user.getRole()), user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用",
                        formatDate(user.getCreatedAt())});
            }
            selectedUser[0] = null;
            statusButton.setEnabled(false);
            roleButton.setEnabled(false);
            detailArea.setText(users.isEmpty() ? "没有找到符合条件的用户。" : "查询到 " + users.size() + " 个用户，请选择查看详情。");
            setStatus("查询到 " + users.size() + " 个用户");
        });

        table.getSelectionModel().addListSelectionListener(event -> {
            int viewRow = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && viewRow >= 0) {
                selectedUser[0] = visibleUsers.get(table.convertRowIndexToModel(viewRow));
                User target = selectedUser[0];
                targetStatusBox.setSelectedIndex(target.getStatus() != null && target.getStatus() == 1 ? 1 : 0);
                targetRoleBox.setSelectedIndex("ADMIN".equals(target.getRole()) ? 1 : 0);
                statusButton.setEnabled(true);
                roleButton.setEnabled(true);
                runAdminTask("加载用户详情", () -> adminUserService.getUserDetail(
                        requireCurrentUserId(), target.getUserId()), detail -> detailArea.setText(formatAdminUserDetail(detail)));
            }
        });

        queryButton.addActionListener(event -> refreshUsers.run());
        resetButton.addActionListener(event -> {
            usernameField.setText("");
            emailField.setText("");
            roleFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            refreshUsers.run();
        });
        usernameField.addActionListener(event -> refreshUsers.run());
        emailField.addActionListener(event -> refreshUsers.run());
        statusButton.addActionListener(event -> runAdminTask("更新用户状态", () -> adminUserService.changeUserStatus(
                requireCurrentUserId(), requireSelectedUser(selectedUser).getUserId(), targetStatusBox.getSelectedIndex()
        ), result -> {
            setStatus(result.message());
            refreshUsers.run();
        }));
        roleButton.addActionListener(event -> runAdminTask("更新用户角色", () -> adminUserService.changeUserRole(
                requireCurrentUserId(), requireSelectedUser(selectedUser).getUserId(),
                targetRoleBox.getSelectedIndex() == 1 ? "ADMIN" : "USER"
        ), result -> {
            setStatus(result.message());
            refreshUsers.run();
        }));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrapWithTitle("用户列表", new JScrollPane(table)), wrapWithTitle("用户详情与操作", detailPanel));
        split.setResizeWeight(0.62);
        split.setDividerLocation(760);
        panel.add(wrapWithTitle("用户筛选", filters), BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAdmissionManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        JTextField orderIdField = new JTextField(10);
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        JTextField noteField = new JTextField(24);
        JButton queryButton = secondaryButton("查询核销记录");
        JButton admitButton = primaryButton("确认核销");
        JPanel toolbar = toolbar();
        toolbar.add(new JLabel("订单ID"));
        toolbar.add(orderIdField);
        toolbar.add(new JLabel("本次数量"));
        toolbar.add(quantitySpinner);
        toolbar.add(new JLabel("备注"));
        toolbar.add(noteField);
        toolbar.add(queryButton);
        toolbar.add(admitButton);

        DefaultTableModel model = tableModel("核销ID", "订单ID", "数量", "操作员ID", "核销时间", "备注");
        JTable table = createTable(model);
        setColumnWidths(table, 75, 80, 65, 90, 165, 230);
        Runnable refresh = () -> runAdminTask("查询核销记录", () -> admissionService.listByOrder(
                requireCurrentUserId(), parseRequiredLong(orderIdField.getText(), "订单ID")), admissions -> {
            model.setRowCount(0);
            for (Admission admission : admissions) {
                model.addRow(new Object[]{admission.getAdmissionId(), admission.getOrderId(), admission.getQuantity(),
                        admission.getOperatorUserId(), formatDate(admission.getAdmittedAt()), valueText(admission.getNote())});
            }
            setStatus("已加载 " + admissions.size() + " 条核销记录");
        });
        queryButton.addActionListener(event -> refresh.run());
        orderIdField.addActionListener(event -> refresh.run());
        admitButton.addActionListener(event -> runAdminTask("门票核销", () -> admissionService.admit(
                requireCurrentUserId(), parseRequiredLong(orderIdField.getText(), "订单ID"),
                (Integer) quantitySpinner.getValue(), noteField.getText()), result -> {
            setStatus(result.message());
            refresh.run();
        }));
        panel.add(wrapWithTitle("管理员门票核销（仅游玩日期当天的已支付订单）", toolbar), BorderLayout.NORTH);
        panel.add(wrapWithTitle("核销记录", new JScrollPane(table)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTicketInventoryManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        JTabbedPane tabs = new JTabbedPane();

        JPanel typePage = new JPanel(new BorderLayout(10, 10));
        typePage.setOpaque(false);
        JTextField itemIdField = new JTextField("1", 7);
        JTextField typeNameField = new JTextField(12);
        JTextField typePriceField = new JTextField("80.00", 8);
        JTextField typeDiscountField = new JTextField("0", 6);
        JComboBox<String> typeStatusBox = new JComboBox<>(new String[]{"下架", "上架"});
        typeStatusBox.setSelectedIndex(1);
        JButton queryTypesButton = primaryButton("查询票种");
        JButton createTypeButton = primaryButton("新增票种");
        JButton updateTypeButton = secondaryButton("更新所选票种");
        updateTypeButton.setEnabled(false);
        JPanel typeToolbar = toolbar();
        typeToolbar.add(new JLabel("景点ID"));
        typeToolbar.add(itemIdField);
        typeToolbar.add(new JLabel("票种名称"));
        typeToolbar.add(typeNameField);
        typeToolbar.add(new JLabel("原价"));
        typeToolbar.add(typePriceField);
        typeToolbar.add(new JLabel("优惠减免%"));
        typeToolbar.add(typeDiscountField);
        typeToolbar.add(new JLabel("状态"));
        typeToolbar.add(typeStatusBox);
        typeToolbar.add(queryTypesButton);
        typeToolbar.add(createTypeButton);
        typeToolbar.add(updateTypeButton);

        DefaultTableModel typeModel = tableModel("票种ID", "景点ID", "票种名称", "原价", "优惠", "折后价", "状态");
        JTable typeTable = createTable(typeModel);
        setColumnWidths(typeTable, 75, 75, 130, 90, 80, 90, 70);
        List<TicketType> visibleTypes = new ArrayList<>();
        TicketType[] selectedType = new TicketType[1];

        JTextField inventoryTypeIdField = new JTextField(8);
        Runnable refreshTypes = () -> runAdminTask("查询票种", () -> ticketInventoryService.listTicketTypes(
                requireCurrentUserId(), parseRequiredLong(itemIdField.getText(), "景点ID"), true), types -> {
            typeModel.setRowCount(0);
            visibleTypes.clear();
            visibleTypes.addAll(types);
            for (TicketType type : types) {
                typeModel.addRow(new Object[]{type.getTicketTypeId(), type.getItemId(), type.getName(),
                        UiFormatters.money(type.getOriginalPrice()), discountText(type.getDiscountRate()),
                        UiFormatters.money(ticketInventoryService.discountedPrice(type)),
                        type.getStatus() != null && type.getStatus() == 1 ? "上架" : "下架"});
            }
            selectedType[0] = null;
            updateTypeButton.setEnabled(false);
            setStatus("已加载 " + types.size() + " 个票种");
        });
        typeTable.getSelectionModel().addListSelectionListener(event -> {
            int row = typeTable.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                TicketType type = visibleTypes.get(typeTable.convertRowIndexToModel(row));
                selectedType[0] = type;
                itemIdField.setText(String.valueOf(type.getItemId()));
                typeNameField.setText(type.getName());
                typePriceField.setText(type.getOriginalPrice().toPlainString());
                typeDiscountField.setText(type.getDiscountRate().stripTrailingZeros().toPlainString());
                typeStatusBox.setSelectedIndex(type.getStatus() != null && type.getStatus() == 1 ? 1 : 0);
                inventoryTypeIdField.setText(String.valueOf(type.getTicketTypeId()));
                updateTypeButton.setEnabled(true);
            }
        });
        queryTypesButton.addActionListener(event -> refreshTypes.run());
        createTypeButton.addActionListener(event -> runAdminTask("新增票种", () -> ticketInventoryService.createTicketType(
                requireCurrentUserId(), parseRequiredLong(itemIdField.getText(), "景点ID"), typeNameField.getText(),
                parseRequiredAmount(typePriceField.getText(), "票价"),
                parseRequiredAmount(typeDiscountField.getText(), "优惠减免比例")), id -> {
            setStatus("票种创建成功，编号：" + id);
            refreshTypes.run();
        }));
        updateTypeButton.addActionListener(event -> runAdminTask("更新票种", () -> {
            if (selectedType[0] == null) {
                throw new IllegalArgumentException("请先选择票种");
            }
            return ticketInventoryService.updateTicketType(requireCurrentUserId(), selectedType[0].getTicketTypeId(),
                    typeNameField.getText(), parseRequiredAmount(typePriceField.getText(), "票价"),
                    parseRequiredAmount(typeDiscountField.getText(), "优惠减免比例"), typeStatusBox.getSelectedIndex());
        }, updated -> {
            setStatus(updated ? "票种已更新" : "票种没有变化");
            refreshTypes.run();
        }));
        typePage.add(wrapWithTitle("票种维护", typeToolbar), BorderLayout.NORTH);
        typePage.add(wrapWithTitle("票种列表", new JScrollPane(typeTable)), BorderLayout.CENTER);

        JPanel inventoryPage = new JPanel(new BorderLayout(10, 10));
        inventoryPage.setOpaque(false);
        JTextField startDateField = new JTextField(LocalDate.now().toString(), 10);
        JTextField endDateField = new JTextField(LocalDate.now().plusDays(14).toString(), 10);
        JTextField maintainDateField = new JTextField(LocalDate.now().plusDays(1).toString(), 10);
        JTextField totalStockField = new JTextField("100", 8);
        JButton queryInventoryButton = primaryButton("查询库存");
        JButton saveInventoryButton = primaryButton("设置总库存");
        JPanel inventoryToolbar = toolbar();
        inventoryToolbar.add(new JLabel("票种ID"));
        inventoryToolbar.add(inventoryTypeIdField);
        inventoryToolbar.add(new JLabel("开始日期"));
        inventoryToolbar.add(startDateField);
        inventoryToolbar.add(new JLabel("结束日期"));
        inventoryToolbar.add(endDateField);
        inventoryToolbar.add(queryInventoryButton);
        inventoryToolbar.add(new JLabel("维护日期"));
        inventoryToolbar.add(maintainDateField);
        inventoryToolbar.add(new JLabel("总库存"));
        inventoryToolbar.add(totalStockField);
        inventoryToolbar.add(saveInventoryButton);

        DefaultTableModel inventoryModel = tableModel("库存ID", "票种ID", "游玩日期", "总库存", "可售", "已预留", "已售", "版本");
        JTable inventoryTable = createTable(inventoryModel);
        setColumnWidths(inventoryTable, 75, 75, 110, 80, 80, 80, 80, 70);
        Runnable refreshInventory = () -> runAdminTask("查询每日库存", () -> ticketInventoryService.listInventory(
                requireCurrentUserId(), parseRequiredLong(inventoryTypeIdField.getText(), "票种ID"),
                parseRequiredDate(startDateField.getText(), "开始日期"),
                parseRequiredDate(endDateField.getText(), "结束日期")), inventories -> {
            inventoryModel.setRowCount(0);
            for (TicketInventory inventory : inventories) {
                inventoryModel.addRow(new Object[]{inventory.getInventoryId(), inventory.getTicketTypeId(),
                        inventory.getVisitDate(), inventory.getTotalStock(), inventory.getAvailableStock(),
                        inventory.getReservedStock(), inventory.getSoldStock(), inventory.getVersion()});
            }
            setStatus("已加载 " + inventories.size() + " 条每日库存");
        });
        queryInventoryButton.addActionListener(event -> refreshInventory.run());
        saveInventoryButton.addActionListener(event -> runAdminTask("设置每日库存", () ->
                ticketInventoryService.setTotalStock(requireCurrentUserId(),
                        parseRequiredLong(inventoryTypeIdField.getText(), "票种ID"),
                        parseRequiredDate(maintainDateField.getText(), "维护日期"),
                        parseRequiredInt(totalStockField.getText(), "总库存")), inventory -> {
            setStatus("库存已保存，可售 " + inventory.getAvailableStock() + " 张");
            refreshInventory.run();
        }));
        inventoryPage.add(wrapWithTitle("按日期维护库存（日期格式 yyyy-MM-dd）", inventoryToolbar), BorderLayout.NORTH);
        inventoryPage.add(wrapWithTitle("每日库存列表", new JScrollPane(inventoryTable)), BorderLayout.CENTER);

        tabs.addTab("票种管理", typePage);
        tabs.addTab("每日库存", inventoryPage);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReportPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField yearField = new JTextField(String.valueOf(LocalDate.now().getYear()), 6);
        JTextField monthField = new JTextField(String.valueOf(LocalDate.now().getMonthValue()), 4);
        JTextField userIdField = new JTextField(10);
        DefaultTableModel monthlyModel = tableModel("日期", "订单数", "销售金额");
        DefaultTableModel hotModel = tableModel("排名", "景点名称", "景点ID", "状态", "总操作", "浏览", "下单", "平均停留(秒)");
        DefaultTableModel userModel = tableModel("指标", "数据");
        DefaultTableModel dashboardModel = tableModel("模块", "指标", "数据");
        JTable monthlyTable = createTable(monthlyModel);
        JTable hotTable = createTable(hotModel);
        JTable userTable = createTable(userModel);
        JTable dashboardTable = createTable(dashboardModel);
        monthlyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        hotTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        userTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        dashboardTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JTabbedPane resultTabs = new JTabbedPane(JTabbedPane.TOP);
        resultTabs.addTab("月度订单", new JScrollPane(monthlyTable));
        resultTabs.addTab("热门排行", new JScrollPane(hotTable));
        resultTabs.addTab(isCurrentAdmin() ? "用户报告" : "我的报告", new JScrollPane(userTable));
        if (isCurrentAdmin()) {
            resultTabs.addTab("综合汇总", new JScrollPane(dashboardTable));
        }

        JPanel reportFilters = toolbar();
        JPanel reportActions = toolbar();
        JPanel reportToolbar = new JPanel(new GridLayout(2, 1, 0, 4));
        reportToolbar.setOpaque(false);
        JButton monthlyButton = primaryButton("月度订单");
        JButton hotButton = secondaryButton("热门排行");
        JButton userButton = secondaryButton(isCurrentAdmin() ? "用户报告" : "我的报告");
        JButton dashboardButton = secondaryButton("综合汇总");
        JButton refreshButton = secondaryButton("刷新当前报表");
        reportFilters.add(new JLabel("年份"));
        reportFilters.add(yearField);
        reportFilters.add(new JLabel("月份"));
        reportFilters.add(monthField);
        if (isCurrentAdmin()) {
            reportFilters.add(new JLabel("用户ID"));
            reportFilters.add(userIdField);
        }
        reportActions.add(monthlyButton);
        reportActions.add(hotButton);
        reportActions.add(userButton);
        if (isCurrentAdmin()) {
            reportActions.add(dashboardButton);
        }
        reportActions.add(refreshButton);
        reportToolbar.add(reportFilters);
        reportToolbar.add(reportActions);

        Runnable loadMonthly = () -> runTask("月度订单报表", () -> statisticsService.getMonthlyOrderReport(
                parseRequiredInt(yearField.getText(), "年份"),
                parseRequiredInt(monthField.getText(), "月份")
        ), reports -> {
            monthlyModel.setRowCount(0);
            for (MonthlyOrderReportDTO report : reports) {
                monthlyModel.addRow(new Object[]{report.getOrderDate(), report.getOrderCount(),
                        UiFormatters.money(report.getTotalAmount())});
            }
            resultTabs.setSelectedIndex(0);
            setStatus("月度订单报表已更新");
        });
        monthlyButton.addActionListener(event -> loadMonthly.run());

        Runnable loadHot = () -> runTask("热门排行", () -> statisticsService.getHotItemRanking(null, null, 10), documents -> {
            hotModel.setRowCount(0);
            int rank = 1;
            for (HotItemRankingDTO ranking : documents) {
                hotModel.addRow(new Object[]{rank, ranking.getItemTitle(), ranking.getItemId(),
                        ranking.isItemFound() ? formatItemStatus(ranking.getItemStatus()) : "-",
                        ranking.getTotalActions(), ranking.getViewCount(), ranking.getOrderCount(),
                        decimalText(ranking.getAvgDuration())});
                rank += 1;
            }
            resultTabs.setSelectedIndex(1);
            setStatus("热门排行已更新");
        });
        hotButton.addActionListener(event -> loadHot.run());

        Runnable loadUserReport = () -> runTask("用户报告", () -> statisticsService.getUserReport(
                requireCurrentUserId(),
                isCurrentAdmin() && !userIdField.getText().isBlank()
                        ? parseRequiredLong(userIdField.getText(), "用户ID")
                        : requireCurrentUserId(),
                null,
                null
        ), document -> {
            fillUserReportTable(userModel, document);
            resultTabs.setSelectedIndex(2);
            setStatus("用户报告已更新");
        });
        userButton.addActionListener(event -> loadUserReport.run());

        Runnable loadDashboard = () -> runTask("综合汇总", () -> statisticsService.buildDashboardReport(
                requireCurrentUserId(), null, null, parseRequiredInt(yearField.getText(), "年份"),
                parseRequiredInt(monthField.getText(), "月份")
        ), dto -> {
            fillDashboardTable(dashboardModel, dto);
            resultTabs.setSelectedIndex(3);
            setStatus("综合汇总已更新");
        });
        dashboardButton.addActionListener(event -> loadDashboard.run());
        refreshButton.addActionListener(event -> {
            int index = resultTabs.getSelectedIndex();
            if (index == 0) {
                loadMonthly.run();
            } else if (index == 1) {
                loadHot.run();
            } else if (index == 2) {
                loadUserReport.run();
            } else if (isCurrentAdmin()) {
                loadDashboard.run();
            }
        });

        panel.add(wrapWithTitle("报表条件", reportToolbar), BorderLayout.NORTH);
        panel.add(wrapWithTitle("报表数据", resultTabs), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAuditPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField userIdField = new JTextField(10);
        JTextField startDateField = new JTextField(10);
        JTextField endDateField = new JTextField(10);
        JTextField keywordField = new JTextField(12);
        JTextField limitField = new JTextField("80", 5);
        JComboBox<String> logTypeBox = new JComboBox<>(new String[]{"全部类型", "登录", "退出", "注册", "创建订单", "景点更新", "查看报表"});
        JComboBox<String> levelBox = new JComboBox<>(new String[]{"全部级别", "正常", "警告", "错误"});
        DefaultTableModel logModel = tableModel("时间", "用户ID", "类型", "级别", "内容", "操作", "IP地址");
        DefaultTableModel summaryModel = tableModel("类型", "级别", "操作次数", "涉及用户", "最近时间");
        DefaultTableModel trendModel = tableModel("日期", "类型", "级别", "次数");
        DefaultTableModel userSummaryModel = tableModel("用户ID", "操作次数", "警告", "错误", "最近时间", "操作类型");
        JTable logTable = createTable(logModel);
        JTable summaryTable = createTable(summaryModel);
        JTable trendTable = createTable(trendModel);
        JTable userSummaryTable = createTable(userSummaryModel);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        summaryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        trendTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        userSummaryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane logScroll = new JScrollPane(logTable);
        JScrollPane summaryScroll = new JScrollPane(summaryTable);
        JScrollPane trendScroll = new JScrollPane(trendTable);
        JScrollPane userSummaryScroll = new JScrollPane(userSummaryTable);
        JTabbedPane auditTabs = new JTabbedPane(JTabbedPane.TOP);
        auditTabs.addTab("日志明细", logScroll);
        auditTabs.addTab("审计汇总", summaryScroll);
        auditTabs.addTab("审计趋势", trendScroll);
        auditTabs.addTab("用户操作", userSummaryScroll);

        JPanel auditFilters = toolbar();
        JPanel auditMoreFilters = toolbar();
        JPanel auditActions = toolbar();
        JPanel auditToolbar = new JPanel(new GridLayout(3, 1, 0, 4));
        auditToolbar.setOpaque(false);
        JButton queryButton = primaryButton("查询日志");
        JButton summaryButton = secondaryButton("审计汇总");
        JButton trendButton = secondaryButton("审计趋势");
        JButton userSummaryButton = secondaryButton("用户操作");
        JButton refreshButton = secondaryButton("刷新当前结果");
        JButton clearButton = secondaryButton("清空条件");
        auditFilters.add(new JLabel("用户ID"));
        auditFilters.add(userIdField);
        auditFilters.add(new JLabel("类型"));
        auditFilters.add(logTypeBox);
        auditFilters.add(new JLabel("级别"));
        auditFilters.add(levelBox);
        auditMoreFilters.add(new JLabel("开始日期"));
        auditMoreFilters.add(startDateField);
        auditMoreFilters.add(new JLabel("结束日期"));
        auditMoreFilters.add(endDateField);
        auditMoreFilters.add(new JLabel("关键词"));
        auditMoreFilters.add(keywordField);
        auditMoreFilters.add(new JLabel("条数"));
        auditMoreFilters.add(limitField);
        auditActions.add(queryButton);
        auditActions.add(summaryButton);
        auditActions.add(trendButton);
        auditActions.add(userSummaryButton);
        auditActions.add(refreshButton);
        auditActions.add(clearButton);
        auditToolbar.add(auditFilters);
        auditToolbar.add(auditMoreFilters);
        auditToolbar.add(auditActions);

        Runnable refreshAuditLogs = () -> runAdminTask("审计日志查询", () -> systemLogService.queryAuditLogs(
                requireCurrentUserId(), buildAuditQuery(userIdField, logTypeBox, levelBox,
                        startDateField, endDateField, keywordField, limitField)), documents -> {
            int scrollPosition = logScroll.getVerticalScrollBar().getValue();
            logModel.setRowCount(0);
            for (Document document : documents) {
                Document detail = document.get("action_detail", Document.class);
                logModel.addRow(new Object[]{formatDate(document.get("timestamp")), valueText(document.get("user_id")),
                        logTypeName(document.getString("log_type")), logLevelName(document.getString("log_level")),
                        valueText(document.get("message")), detail == null ? "-" : valueText(detail.get("operation")),
                        detail == null ? "-" : valueText(detail.get("ip"))});
            }
            auditTabs.setSelectedIndex(0);
            restoreScrollPosition(logScroll, scrollPosition);
            setStatus("查询到 " + documents.size() + " 条审计日志");
        });
        queryButton.addActionListener(event -> refreshAuditLogs.run());

        Runnable loadSummary = () -> runAdminTask("审计汇总", () -> systemLogService.getAuditSummary(
                requireCurrentUserId(), parseOptionalStartDate(startDateField.getText(), "开始日期"),
                parseOptionalEndDate(endDateField.getText(), "结束日期")), documents -> {
            int scrollPosition = summaryScroll.getVerticalScrollBar().getValue();
            summaryModel.setRowCount(0);
            for (Document document : documents) {
                summaryModel.addRow(new Object[]{logTypeName(document.getString("log_type")),
                        logLevelName(document.getString("log_level")), numberText(document.get("operation_count")),
                        numberText(document.get("user_count")), formatDate(document.get("latest_timestamp"))});
            }
            auditTabs.setSelectedIndex(1);
            restoreScrollPosition(summaryScroll, scrollPosition);
        });
        summaryButton.addActionListener(event -> loadSummary.run());

        Runnable loadTrend = () -> runAdminTask("审计趋势", () -> systemLogService.getDailyAuditTrend(
                requireCurrentUserId(), parseOptionalStartDate(startDateField.getText(), "开始日期"),
                parseOptionalEndDate(endDateField.getText(), "结束日期")), documents -> {
            int scrollPosition = trendScroll.getVerticalScrollBar().getValue();
            trendModel.setRowCount(0);
            for (Document document : documents) {
                trendModel.addRow(new Object[]{valueText(document.get("date")), logTypeName(document.getString("log_type")),
                        logLevelName(document.getString("log_level")), numberText(document.get("operation_count"))});
            }
            auditTabs.setSelectedIndex(2);
            restoreScrollPosition(trendScroll, scrollPosition);
        });
        trendButton.addActionListener(event -> loadTrend.run());

        Runnable loadUserSummary = () -> runAdminTask("用户操作汇总",
                () -> systemLogService.getUserOperationSummary(requireCurrentUserId(),
                        parseOptionalStartDate(startDateField.getText(), "开始日期"),
                        parseOptionalEndDate(endDateField.getText(), "结束日期"),
                        parseOptionalInt(limitField.getText(), 50, "条数")), documents -> {
                    int scrollPosition = userSummaryScroll.getVerticalScrollBar().getValue();
                    userSummaryModel.setRowCount(0);
                    for (Document document : documents) {
                        userSummaryModel.addRow(new Object[]{valueText(document.get("user_id")),
                                numberText(document.get("operation_count")), numberText(document.get("warn_count")),
                                numberText(document.get("error_count")), formatDate(document.get("latest_timestamp")),
                                logTypeListText(document.get("log_types"))});
                    }
                    auditTabs.setSelectedIndex(3);
                    restoreScrollPosition(userSummaryScroll, scrollPosition);
                });
        userSummaryButton.addActionListener(event -> loadUserSummary.run());
        clearButton.addActionListener(event -> {
            userIdField.setText("");
            startDateField.setText("");
            endDateField.setText("");
            keywordField.setText("");
            limitField.setText("80");
            logTypeBox.setSelectedIndex(0);
            levelBox.setSelectedIndex(0);
            refreshAuditLogs.run();
        });
        refreshButton.addActionListener(event -> {
            switch (auditTabs.getSelectedIndex()) {
                case 0 -> refreshAuditLogs.run();
                case 1 -> loadSummary.run();
                case 2 -> loadTrend.run();
                case 3 -> loadUserSummary.run();
                default -> refreshAuditLogs.run();
            }
        });
        userIdField.addActionListener(event -> refreshAuditLogs.run());
        keywordField.addActionListener(event -> refreshAuditLogs.run());

        panel.add(wrapWithTitle("审计条件", auditToolbar), BorderLayout.NORTH);
        panel.add(wrapWithTitle("审计结果", auditTabs), BorderLayout.CENTER);
        return panel;
    }

    private void setCurrentUser(LoginResult result) {
        sessionTaskGuard.advanceSession();
        currentUser = result.getUser();
        updateSessionLabel();
        refreshHomeSummary();
        setStatus("登录成功：" + currentUser.getUsername());
    }

    private void updateSessionLabel() {
        if (currentUser == null) {
            userLabel.setText("未登录");
            return;
        }
        userLabel.setText(currentUser.getUsername() + "  ·  " + roleDisplay(currentUser.getRole()));
    }

    private void refreshHomeSummary() {
        if (currentUser == null) {
            homeUserValue.setText("未登录");
            homeRoleValue.setText("-");
        } else {
            homeUserValue.setText(currentUser.getUsername() + " / ID " + currentUser.getUserId());
            homeRoleValue.setText(roleDisplay(currentUser.getRole()));
        }
        homeSummaryArea.setText(currentUser == null ? "请登录后使用系统。"
                : "你好，" + currentUser.getUsername() + "。请选择上方快捷入口或左侧导航开始使用。"
                + (isCurrentAdmin() ? System.lineSeparator() + "当前为管理员账户，可使用后台管理与系统审计。" : ""));
    }

    private void fillProfileForm(Profile profile, JTextField userIdField, JTextField realNameField,
                                 JTextField idCardField, JTextField addressField, JTextArea notesArea) {
        userIdField.setText(valueText(profile.getUserId()));
        realNameField.setText(fieldText(profile.getRealName()));
        idCardField.setText(fieldText(profile.getIdCard()));
        addressField.setText(fieldText(profile.getAddress()));
        notesArea.setText(fieldText(profile.getNotes()));
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

    private User requireSelectedUser(User[] selectedUser) {
        if (selectedUser == null || selectedUser.length == 0 || selectedUser[0] == null) {
            throw new IllegalArgumentException("请先从表格中选择一个用户");
        }
        return selectedUser[0];
    }

    private long requireSelectedOrderId(Long[] selectedOrderId) {
        if (selectedOrderId == null || selectedOrderId.length == 0 || selectedOrderId[0] == null) {
            throw new IllegalArgumentException("请先从表格中选择一个订单");
        }
        return selectedOrderId[0];
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

    private JPanel metricCard(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(100, 110, 120));
        valueLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private JButton navButton(String text, String tabTitle) {
        JButton button = secondaryButton(text);
        button.addActionListener(event -> switchTo(tabTitle));
        return button;
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

    private String buildSearchKeyword(String presetKeyword, String customKeyword) {
        String preset = presetKeyword == null || ALL_OPTION.equals(presetKeyword) ? "" : presetKeyword.trim();
        String custom = customKeyword == null ? "" : customKeyword.trim();
        return custom.isBlank() ? preset : custom;
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

    private String formatRecommendationScore(double score) {
        return UiFormatters.recommendationScore(score);
    }

    private Integer selectedOrderStatus(JComboBox<String> statusBox) {
        int selectedIndex = statusBox.getSelectedIndex();
        return selectedIndex <= 0 ? null : selectedIndex - 1;
    }

    private String selectedLogType(JComboBox<String> logTypeBox) {
        return switch (logTypeBox.getSelectedIndex()) {
            case 1 -> "LOGIN";
            case 2 -> "LOGOUT";
            case 3 -> "REGISTER";
            case 4 -> "ORDER_CREATE";
            case 5 -> "ITEM_UPDATE";
            case 6 -> "REPORT_VIEW";
            default -> null;
        };
    }

    private String selectedLogLevel(JComboBox<String> levelBox) {
        return switch (levelBox.getSelectedIndex()) {
            case 1 -> "INFO";
            case 2 -> "WARN";
            case 3 -> "ERROR";
            default -> null;
        };
    }

    private AuditLogQuery buildAuditQuery(JTextField userIdField, JComboBox<String> logTypeBox,
                                          JComboBox<String> levelBox, JTextField startDateField,
                                          JTextField endDateField, JTextField keywordField,
                                          JTextField limitField) {
        AuditLogQuery query = new AuditLogQuery();
        query.setUserId(parseOptionalLong(userIdField.getText()));
        query.setLogType(selectedLogType(logTypeBox));
        query.setLogLevel(selectedLogLevel(levelBox));
        query.setStartTime(parseOptionalStartDate(startDateField.getText(), "开始日期"));
        query.setEndTime(parseOptionalEndDate(endDateField.getText(), "结束日期"));
        query.setKeyword(keywordField.getText());
        query.setLimit(parseOptionalInt(limitField.getText(), 80, "条数"));
        return query;
    }

    private String selectedUserRoleFilter(JComboBox<String> roleBox) {
        return switch (roleBox.getSelectedIndex()) {
            case 1 -> "ADMIN";
            case 2 -> "USER";
            default -> null;
        };
    }

    private Integer selectedUserStatusFilter(JComboBox<String> statusBox) {
        return switch (statusBox.getSelectedIndex()) {
            case 1 -> 1;
            case 2 -> 0;
            default -> null;
        };
    }

    private void restoreScrollPosition(JScrollPane scrollPane, int position) {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(position));
    }

    private void fillUserReportTable(DefaultTableModel model, Document document) {
        model.setRowCount(0);
        if (document == null || document.isEmpty()) {
            return;
        }
        addMetricRow(model, "用户ID", valueText(document.get("user_id")));
        addMetricRow(model, "总操作次数", numberText(document.get("action_count")));
        addMetricRow(model, "访问景点数", numberText(document.get("visited_item_count")));
        addMetricRow(model, "浏览次数", numberText(document.get("view_count")));
        addMetricRow(model, "搜索次数", numberText(document.get("search_count")));
        addMetricRow(model, "评论次数", numberText(document.get("comment_count")));
        addMetricRow(model, "下单次数", numberText(document.get("order_count")));
        addMetricRow(model, "总停留时长", numberText(document.get("total_duration")) + " 秒");
        addMetricRow(model, "平均停留时长", decimalText(document.get("avg_duration")) + " 秒");
        addMetricRow(model, "首次操作", formatDate(document.get("first_action_time")));
        addMetricRow(model, "最近操作", formatDate(document.get("latest_action_time")));
    }

    private void addMetricRow(DefaultTableModel model, String name, Object value) {
        model.addRow(new Object[]{name, value});
    }

    private void fillDashboardTable(DefaultTableModel model, StatisticsReportDTO dto) {
        model.setRowCount(0);
        for (HotItemRankingDTO ranking : dto.getHotItems()) {
            model.addRow(new Object[]{"热门景点", ranking.getItemTitle() + " / ID " + ranking.getItemId(),
                    "总操作 " + ranking.getTotalActions() + "，浏览 "
                            + ranking.getViewCount() + "，下单 " + ranking.getOrderCount()});
        }
        for (Document document : dto.getActionTypeSummary()) {
            model.addRow(new Object[]{"用户行为", actionTypeName(document.getString("action_type")),
                    numberText(document.get("action_count")) + " 次"});
        }
        for (Document document : dto.getHotTags()) {
            model.addRow(new Object[]{"热门标签", valueText(document.get("_id")),
                    numberText(document.get("tag_count")) + " 次"});
        }
        for (Document document : dto.getSystemAuditSummary()) {
            model.addRow(new Object[]{"系统审计", logTypeName(document.getString("log_type")) + " / "
                    + logLevelName(document.getString("log_level")), numberText(document.get("operation_count")) + " 次"});
        }
        for (MonthlyOrderReportDTO report : dto.getMonthlyOrderReport()) {
            model.addRow(new Object[]{"月度订单", report.getOrderDate(),
                    report.getOrderCount() + " 单，" + UiFormatters.money(report.getTotalAmount())});
        }
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

    private String formatAdminUserDetail(AdminUserDetailDTO detail) {
        User user = detail.getUser();
        Profile profile = detail.getProfile();
        var orders = detail.getOrderSummary();
        StringBuilder builder = new StringBuilder("用户基本信息")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("用户ID：").append(valueText(user.getUserId())).append(System.lineSeparator())
                .append("用户名：").append(valueText(user.getUsername())).append(System.lineSeparator())
                .append("邮箱：").append(valueText(user.getEmail())).append(System.lineSeparator())
                .append("手机号：").append(valueText(user.getPhone())).append(System.lineSeparator())
                .append("角色：").append(roleDisplay(user.getRole())).append(System.lineSeparator())
                .append("状态：").append(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("用户档案").append(System.lineSeparator());
        if (profile == null) {
            builder.append("暂无档案").append(System.lineSeparator());
        } else {
            builder.append("真实姓名：").append(valueText(profile.getRealName())).append(System.lineSeparator())
                    .append("证件号：").append(valueText(profile.getIdCard())).append(System.lineSeparator())
                    .append("地址：").append(valueText(profile.getAddress())).append(System.lineSeparator())
                    .append("备注：").append(valueText(profile.getNotes())).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("订单概况").append(System.lineSeparator())
                .append("总订单：").append(orders.getTotalOrders())
                .append("，待支付：").append(orders.getPendingOrders())
                .append("，已支付：").append(orders.getPaidOrders())
                .append("，已取消：").append(orders.getCancelledOrders())
                .append("，已完成：").append(orders.getCompletedOrders()).append(System.lineSeparator())
                .append("有效订单金额：").append(UiFormatters.money(orders.getPaidAmount()))
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append("行为概况").append(System.lineSeparator())
                .append(detail.isBehaviorDataAvailable()
                        ? "行为日志数量：" + detail.getBehaviorCount()
                        : "MongoDB 行为数据暂不可用，MySQL 用户信息仍可管理");
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

    private String formatMonthlyReports(List<MonthlyOrderReportDTO> reports) {
        StringBuilder builder = new StringBuilder();
        for (MonthlyOrderReportDTO report : reports) {
            builder.append(report.getOrderDate())
                    .append(" 订单数：").append(report.getOrderCount())
                    .append(" 金额：").append(report.getTotalAmount())
                    .append(System.lineSeparator());
        }
        return builder.isEmpty() ? "暂无报表数据" : builder.toString();
    }

    private String formatDocuments(List<Document> documents) {
        StringBuilder builder = new StringBuilder();
        for (Document document : documents) {
            builder.append(documentToText(document)).append(System.lineSeparator());
        }
        return builder.isEmpty() ? "暂无数据" : builder.toString();
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

    private String formatBehaviorSummary(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无行为记录";
        }
        StringBuilder builder = new StringBuilder();
        for (Document document : documents) {
            builder.append("行为：").append(actionTypeName(String.valueOf(document.get("_id"))))
                    .append("  次数：").append(numberText(document.get("action_count")))
                    .append("  总停留：").append(numberText(document.get("total_duration"))).append(" 秒")
                    .append("  平均停留：").append(decimalText(document.get("avg_duration"))).append(" 秒")
                    .append("  最近时间：").append(formatDate(document.get("latest_action_time")))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String formatHotItems(List<HotItemRankingDTO> rankings) {
        if (rankings == null || rankings.isEmpty()) {
            return "暂无热门景点数据";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (HotItemRankingDTO ranking : rankings) {
            builder.append(index).append(". 景点：").append(ranking.getItemTitle())
                    .append("  ID：").append(ranking.getItemId())
                    .append("  总操作：").append(ranking.getTotalActions())
                    .append("  浏览：").append(ranking.getViewCount())
                    .append("  下单：").append(ranking.getOrderCount())
                    .append("  平均停留：").append(decimalText(ranking.getAvgDuration())).append(" 秒")
                    .append(System.lineSeparator());
            index += 1;
        }
        return builder.toString();
    }

    private String formatActionTypeSummary(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无行为类型数据";
        }
        StringBuilder builder = new StringBuilder();
        for (Document document : documents) {
            builder.append("行为：").append(actionTypeName(document.getString("action_type")))
                    .append("  次数：").append(numberText(document.get("action_count")))
                    .append("  用户数：").append(numberText(document.get("user_count")))
                    .append("  最近时间：").append(formatDate(document.get("latest_action_time")))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String formatHotTags(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无热门标签数据";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            builder.append(index).append(". 标签：").append(valueText(document.get("_id")))
                    .append("  出现次数：").append(numberText(document.get("tag_count")))
                    .append(System.lineSeparator());
            index += 1;
        }
        return builder.toString();
    }

    private String formatUserReport(Document document) {
        if (document == null || document.isEmpty()) {
            return "暂无用户报告";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("用户报告").append(System.lineSeparator()).append(System.lineSeparator())
                .append("用户ID：").append(valueText(document.get("user_id"))).append(System.lineSeparator())
                .append("总操作次数：").append(numberText(document.get("action_count"))).append(System.lineSeparator())
                .append("访问景点数：").append(numberText(document.get("visited_item_count"))).append(System.lineSeparator())
                .append("浏览次数：").append(numberText(document.get("view_count"))).append(System.lineSeparator())
                .append("搜索次数：").append(numberText(document.get("search_count"))).append(System.lineSeparator())
                .append("评论次数：").append(numberText(document.get("comment_count"))).append(System.lineSeparator())
                .append("下单次数：").append(numberText(document.get("order_count"))).append(System.lineSeparator())
                .append("总停留时长：").append(numberText(document.get("total_duration"))).append(" 秒").append(System.lineSeparator())
                .append("平均停留时长：").append(decimalText(document.get("avg_duration"))).append(" 秒").append(System.lineSeparator())
                .append("首次操作：").append(formatDate(document.get("first_action_time"))).append(System.lineSeparator())
                .append("最近操作：").append(formatDate(document.get("latest_action_time"))).append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("行为摘要：").append(System.lineSeparator())
                .append(formatBehaviorSummary(readDocumentList(document.get("behavior_summary"))))
                .append(System.lineSeparator())
                .append("最近评论：").append(System.lineSeparator())
                .append(formatComments(readDocumentList(document.get("recent_comments"))))
                .append(System.lineSeparator())
                .append("最近操作：").append(System.lineSeparator())
                .append(formatRecentActions(readDocumentList(document.get("recent_actions"))));
        return builder.toString();
    }

    private String formatRecentActions(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无最近操作";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            Document clientInfo = document.get("client_info", Document.class);
            builder.append(index).append(". 时间：").append(formatDate(document.get("created_at")))
                    .append("  行为：").append(actionTypeName(document.getString("action_type")))
                    .append("  景点ID：").append(valueText(document.get("item_id")))
                    .append("  停留：").append(numberText(document.get("duration_seconds"))).append(" 秒");
            if (clientInfo != null) {
                builder.append("  IP：").append(valueText(clientInfo.get("ip")));
            }
            builder.append(System.lineSeparator());
            index += 1;
        }
        return builder.toString();
    }

    private String formatAuditLogs(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无审计日志";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            Document detail = document.get("action_detail", Document.class);
            builder.append(index).append(". ")
                    .append(formatDate(document.get("timestamp")))
                    .append("  用户ID：").append(valueText(document.get("user_id")))
                    .append("  类型：").append(logTypeName(document.getString("log_type")))
                    .append("  级别：").append(logLevelName(document.getString("log_level")))
                    .append(System.lineSeparator())
                    .append("   内容：").append(valueText(document.get("message")))
                    .append(System.lineSeparator());
            if (detail != null && !detail.isEmpty()) {
                builder.append("   操作：").append(valueText(detail.get("operation")))
                        .append("  IP：").append(valueText(detail.get("ip")))
                        .append(System.lineSeparator());
            }
            builder.append(System.lineSeparator());
            index += 1;
        }
        return builder.toString();
    }

    private String formatAuditSummary(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无审计汇总数据";
        }
        StringBuilder builder = new StringBuilder("审计汇总").append(System.lineSeparator()).append(System.lineSeparator());
        for (Document document : documents) {
            builder.append("类型：").append(logTypeName(document.getString("log_type")))
                    .append("  级别：").append(logLevelName(document.getString("log_level")))
                    .append("  操作次数：").append(numberText(document.get("operation_count")))
                    .append("  涉及用户：").append(numberText(document.get("user_count")))
                    .append("  最近时间：").append(formatDate(document.get("latest_timestamp")))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String formatAuditTrend(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无审计趋势数据";
        }
        StringBuilder builder = new StringBuilder("审计趋势").append(System.lineSeparator()).append(System.lineSeparator());
        for (Document document : documents) {
            builder.append("日期：").append(valueText(document.get("date")))
                    .append("  类型：").append(logTypeName(document.getString("log_type")))
                    .append("  级别：").append(logLevelName(document.getString("log_level")))
                    .append("  次数：").append(numberText(document.get("operation_count")))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String formatUserOperationSummary(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无用户操作汇总数据";
        }
        StringBuilder builder = new StringBuilder("用户操作汇总").append(System.lineSeparator()).append(System.lineSeparator());
        for (Document document : documents) {
            builder.append("用户ID：").append(valueText(document.get("user_id")))
                    .append("  操作次数：").append(numberText(document.get("operation_count")))
                    .append("  警告：").append(numberText(document.get("warn_count")))
                    .append("  错误：").append(numberText(document.get("error_count")))
                    .append("  最近时间：").append(formatDate(document.get("latest_timestamp")))
                    .append(System.lineSeparator())
                    .append("   操作类型：").append(logTypeListText(document.get("log_types")))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String documentToText(Document document) {
        if (document == null || document.isEmpty()) {
            return "暂无数据";
        }
        StringBuilder builder = new StringBuilder();
        for (String key : document.keySet()) {
            builder.append(fieldName(key)).append("：").append(genericValueText(document.get(key))).append(System.lineSeparator());
        }
        return builder.toString().trim();
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

    private String listText(Object value) {
        if (value instanceof List<?> values && !values.isEmpty()) {
            return values.stream()
                    .map(String::valueOf)
                    .reduce((left, right) -> left + "、" + right)
                    .orElse("-");
        }
        return "-";
    }

    private List<Document> readDocumentList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<Document> documents = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof Document document) {
                documents.add(document);
            }
        }
        return documents;
    }

    private String genericValueText(Object value) {
        if (value instanceof Document document) {
            return documentToText(document).replace(System.lineSeparator(), "；");
        }
        if (value instanceof List<?> values) {
            if (values.isEmpty()) {
                return "-";
            }
            return values.stream()
                    .map(this::genericValueText)
                    .reduce((left, right) -> left + "、" + right)
                    .orElse("-");
        }
        if (value instanceof java.util.Date) {
            return formatDate(value);
        }
        if (value instanceof Number number && !(value instanceof Integer) && !(value instanceof Long)) {
            return decimalText(number);
        }
        return valueText(value);
    }

    private String logTypeListText(Object value) {
        if (value instanceof List<?> values) {
            return values.stream()
                    .map(item -> logTypeName(String.valueOf(item)))
                    .distinct()
                    .reduce((left, right) -> left + "、" + right)
                    .orElse("-");
        }
        return valueText(value);
    }

    private String logTypeName(String logType) {
        if (logType == null || logType.isBlank()) {
            return "-";
        }
        return switch (logType) {
            case "LOGIN" -> "登录";
            case "LOGOUT" -> "退出";
            case "REGISTER" -> "注册";
            case "ORDER_CREATE", "ORDER" -> "创建订单";
            case "ITEM_UPDATE" -> "景点更新";
            case "REPORT_VIEW" -> "查看报表";
            default -> logType;
        };
    }

    private String actionTypeName(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return "-";
        }
        return switch (actionType) {
            case "VIEW" -> "浏览景点";
            case "SEARCH" -> "搜索";
            case "ORDER" -> "下单";
            case "COMMENT" -> "评论";
            default -> actionType;
        };
    }

    private String fieldName(String key) {
        return switch (key) {
            case "_id" -> "编号";
            case "item_id" -> "景点ID";
            case "user_id" -> "用户ID";
            case "action_type" -> "行为类型";
            case "action_count" -> "操作次数";
            case "total_actions" -> "总操作次数";
            case "view_count" -> "浏览次数";
            case "search_count" -> "搜索次数";
            case "comment_count" -> "评论次数";
            case "order_count" -> "下单次数";
            case "duration_seconds" -> "停留时长";
            case "total_duration" -> "总停留时长";
            case "avg_duration" -> "平均停留时长";
            case "latest_action_time" -> "最近操作时间";
            case "first_action_time" -> "首次操作时间";
            case "created_at" -> "创建时间";
            case "updated_at" -> "更新时间";
            case "content" -> "内容";
            case "rating" -> "评分";
            case "tags" -> "标签";
            case "description" -> "详情";
            case "images" -> "图片";
            case "metadata" -> "扩展信息";
            case "avg_rating" -> "平均评分";
            case "max_rating" -> "最高评分";
            case "min_rating" -> "最低评分";
            case "visited_item_count" -> "访问景点数";
            case "behavior_summary" -> "行为摘要";
            case "recent_comments" -> "最近评论";
            case "recent_actions" -> "最近操作";
            case "client_info" -> "客户端信息";
            case "client_type" -> "客户端类型";
            case "ip" -> "IP地址";
            case "open_time" -> "开放时间";
            case "address" -> "地址";
            case "language" -> "语言";
            default -> key;
        };
    }

    private String logLevelName(String logLevel) {
        if (logLevel == null || logLevel.isBlank()) {
            return "-";
        }
        return switch (logLevel) {
            case "INFO" -> "正常";
            case "WARN" -> "警告";
            case "ERROR" -> "错误";
            default -> logLevel;
        };
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
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID 必须是数字，请检查输入内容", e);
        }
    }

    private long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是数字", e);
        }
    }

    private int parseRequiredInt(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是整数", e);
        }
    }

    private int parseOptionalInt(String value, int defaultValue, String fieldName) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return parseRequiredInt(value, fieldName);
    }

    private BigDecimal parseRequiredAmount(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(fieldName + "不能小于 0");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是有效金额", e);
        }
    }

    private LocalDate parseRequiredDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + "必须使用 yyyy-MM-dd 格式", exception);
        }
    }

    private Date parseOptionalStartDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.from(parseRequiredDate(value, fieldName)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
    }

    private Date parseOptionalEndDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.from(parseRequiredDate(value, fieldName)
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .minusNanos(1)
                .toInstant());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
