package com.scenicticket.ui;

import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.dto.LoginResult;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import com.scenicticket.model.Order;
import com.scenicticket.model.Profile;
import com.scenicticket.model.User;
import com.scenicticket.service.BehaviorLogService;
import com.scenicticket.service.BusinessService;
import com.scenicticket.service.CrossDatabaseQueryService;
import com.scenicticket.service.RecommendService;
import com.scenicticket.service.StatisticsService;
import com.scenicticket.service.SystemLogService;
import com.scenicticket.service.UserService;
import org.bson.Document;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class AppFrame extends JFrame {
    private static final Color BACKGROUND = new Color(246, 248, 250);
    private static final Color PANEL_BORDER = new Color(220, 225, 230);
    private static final Font TITLE_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 22);
    private static final Font SECTION_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 15);

    private final UserService userService = new UserService();
    private final BusinessService businessService = new BusinessService();
    private final CrossDatabaseQueryService crossDatabaseQueryService = new CrossDatabaseQueryService();
    private final RecommendService recommendService = new RecommendService();
    private final StatisticsService statisticsService = new StatisticsService();
    private final SystemLogService systemLogService = new SystemLogService();
    private final BehaviorLogService behaviorLogService = new BehaviorLogService();

    private final JLabel userLabel = new JLabel("未登录");
    private final JLabel statusLabel = new JLabel("就绪");
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
    private final JTextArea homeSummaryArea = createTextArea(10, 80);

    private User currentUser;

    public AppFrame() {
        setTitle("景点售票系统");
        setMinimumSize(new Dimension(1180, 760));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND);

        add(createHeader(), BorderLayout.NORTH);
        add(createPages(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        refreshHomeSummary();
    }

    private Component createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        header.setBackground(Color.WHITE);

        JLabel title = new JLabel("景点售票系统");
        title.setFont(TITLE_FONT);
        JLabel subtitle = new JLabel("MySQL + MongoDB + Java Swing");
        subtitle.setForeground(new Color(100, 110, 120));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(subtitle);

        JPanel sessionBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        sessionBlock.setOpaque(false);
        JButton logoutButton = new JButton("退出登录");
        logoutButton.addActionListener(event -> {
            currentUser = null;
            updateSessionLabel();
            refreshHomeSummary();
            setStatus("已退出登录");
        });
        sessionBlock.add(userLabel);
        sessionBlock.add(logoutButton);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(sessionBlock, BorderLayout.EAST);
        return header;
    }

    private Component createPages() {
        tabs.addTab("首页", createHomePanel());
        tabs.addTab("登录注册", createAuthPanel());
        tabs.addTab("个人档案", createProfilePanel());
        tabs.addTab("景点浏览", createItemPanel());
        tabs.addTab("我的订单", createOrderPanel());
        tabs.addTab("后台管理", createManagePanel());
        tabs.addTab("推荐", createRecommendPanel());
        tabs.addTab("统计报表", createReportPanel());
        tabs.addTab("系统审计", createAuditPanel());
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabs.setBackground(BACKGROUND);
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

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 12));
        metrics.setOpaque(false);
        metrics.add(metricCard("用户状态", () -> currentUser == null ? "未登录" : currentUser.getUsername()));
        metrics.add(metricCard("当前角色", () -> currentUser == null ? "-" : currentUser.getRole()));
        metrics.add(metricCard("核心模块", () -> "9 个页面"));
        metrics.add(metricCard("数据架构", () -> "MySQL + MongoDB"));

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickActions.setOpaque(false);
        quickActions.add(navButton("登录注册", "登录注册"));
        quickActions.add(navButton("浏览景点", "景点浏览"));
        quickActions.add(navButton("查看订单", "我的订单"));
        quickActions.add(navButton("统计报表", "统计报表"));
        quickActions.add(navButton("系统审计", "系统审计"));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(metrics, BorderLayout.CENTER);
        top.add(quickActions, BorderLayout.SOUTH);

        homeSummaryArea.setText("");
        panel.add(top, BorderLayout.NORTH);
        panel.add(wrapWithTitle("系统概览", homeSummaryArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAuthPanel() {
        JPanel panel = pagePanel(new GridLayout(1, 2, 12, 0));

        JTextField loginUsername = new JTextField(22);
        JPasswordField loginPassword = new JPasswordField(22);
        JTextField loginIp = new JTextField("127.0.0.1", 22);
        JButton loginButton = new JButton("登录");
        JLabel loginResult = new JLabel(" ");

        JPanel loginPanel = formPanel("用户登录");
        addField(loginPanel, 0, "用户名", loginUsername);
        addField(loginPanel, 1, "密码", loginPassword);
        addField(loginPanel, 2, "IP", loginIp);
        addFormButton(loginPanel, 3, loginButton);
        addFormMessage(loginPanel, 4, loginResult);

        JTextField registerUsername = new JTextField(22);
        JPasswordField registerPassword = new JPasswordField(22);
        JTextField registerEmail = new JTextField(22);
        JTextField registerPhone = new JTextField(22);
        JButton registerButton = new JButton("注册");
        JLabel registerResult = new JLabel(" ");

        JPanel registerPanel = formPanel("用户注册");
        addField(registerPanel, 0, "用户名", registerUsername);
        addField(registerPanel, 1, "密码", registerPassword);
        addField(registerPanel, 2, "邮箱", registerEmail);
        addField(registerPanel, 3, "手机号", registerPhone);
        addFormButton(registerPanel, 4, registerButton);
        addFormMessage(registerPanel, 5, registerResult);

        loginButton.addActionListener(event -> runTask("用户登录", () -> userService.login(
                loginUsername.getText(),
                new String(loginPassword.getPassword()),
                loginIp.getText()
        ), result -> {
            loginResult.setText(result.getMessage());
            if (result.isSuccess()) {
                setCurrentUser(result);
                switchTo("首页");
            }
        }));

        registerButton.addActionListener(event -> runTask("用户注册", () -> userService.register(
                registerUsername.getText(),
                new String(registerPassword.getPassword()),
                registerEmail.getText(),
                registerPhone.getText()
        ), userId -> {
            registerResult.setText("注册成功，用户ID：" + userId);
            setStatus("注册成功，用户ID：" + userId);
        }));

        panel.add(loginPanel);
        panel.add(registerPanel);
        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField userIdField = new JTextField(12);
        JTextField realNameField = new JTextField(24);
        JTextField idCardField = new JTextField(24);
        JTextField addressField = new JTextField(32);
        JTextArea notesArea = new JTextArea(5, 32);
        JButton saveButton = new JButton("保存档案");
        JLabel message = new JLabel(" ");

        JPanel form = formPanel("个人档案");
        addField(form, 0, "用户ID", userIdField);
        addField(form, 1, "真实姓名", realNameField);
        addField(form, 2, "证件号", idCardField);
        addField(form, 3, "地址", addressField);
        addTextAreaField(form, 4, "备注", notesArea);
        addFormButton(form, 5, saveButton);
        addFormMessage(form, 6, message);

        saveButton.addActionListener(event -> runTask("保存档案", () -> {
            Profile profile = new Profile();
            profile.setUserId(userIdField.getText().isBlank() ? requireCurrentUserId() : parseRequiredLong(userIdField.getText(), "用户ID"));
            profile.setRealName(realNameField.getText());
            profile.setIdCard(idCardField.getText());
            profile.setAddress(addressField.getText());
            profile.setNotes(notesArea.getText());
            return userService.updateProfile(profile);
        }, saved -> message.setText(saved ? "档案已保存" : "档案未更新")));

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createItemPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));

        JTextField keywordField = new JTextField(18);
        JTextField categoryField = new JTextField(8);
        JTextField itemIdField = new JTextField(8);
        JTextField orderAmountField = new JTextField("80.00", 8);
        JTextField ratingField = new JTextField("5", 4);
        JTextField commentField = new JTextField(28);
        JTextArea detailArea = createTextArea(10, 60);
        DefaultTableModel tableModel = tableModel("ID", "标题", "分类", "状态", "创建时间", "更新时间");
        JTable table = createTable(tableModel);

        JPanel toolbar = toolbar();
        JButton searchButton = new JButton("查询景点");
        JButton detailButton = new JButton("查看详情");
        JButton orderButton = new JButton("创建订单");
        JButton commentButton = new JButton("发表评论");
        toolbar.add(new JLabel("关键词"));
        toolbar.add(keywordField);
        toolbar.add(new JLabel("分类ID"));
        toolbar.add(categoryField);
        toolbar.add(searchButton);
        toolbar.add(new JLabel("景点ID"));
        toolbar.add(itemIdField);
        toolbar.add(detailButton);
        toolbar.add(new JLabel("金额"));
        toolbar.add(orderAmountField);
        toolbar.add(orderButton);
        toolbar.add(new JLabel("评分"));
        toolbar.add(ratingField);
        toolbar.add(commentField);
        toolbar.add(commentButton);

        table.getSelectionModel().addListSelectionListener(event -> {
            int row = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                Object value = table.getValueAt(row, 0);
                itemIdField.setText(String.valueOf(value));
            }
        });

        searchButton.addActionListener(event -> runTask("景点查询", () -> businessService.searchItems(
                keywordField.getText(), parseOptionalLong(categoryField.getText()), 50, 0
        ), items -> {
            tableModel.setRowCount(0);
            for (Item item : items) {
                tableModel.addRow(new Object[]{
                        item.getItemId(), item.getTitle(), item.getCategoryId(), formatItemStatus(item.getStatus()),
                        item.getCreatedAt(), item.getUpdatedAt()
                });
            }
            setStatus("查询到 " + items.size() + " 个景点");
        }));

        detailButton.addActionListener(event -> runTask("景点详情", () -> crossDatabaseQueryService.getItemDetail(
                parseRequiredLong(itemIdField.getText(), "景点ID"), 8
        ), dto -> detailArea.setText(formatItemDetail(dto))));

        orderButton.addActionListener(event -> runTask("创建订单", () -> businessService.createOrder(
                requireCurrentUserId(),
                parseRequiredLong(itemIdField.getText(), "景点ID"),
                new BigDecimal(orderAmountField.getText().trim())
        ), orderId -> detailArea.setText("订单创建成功，订单ID：" + orderId)));

        commentButton.addActionListener(event -> runTask("发表评论", () -> {
            behaviorLogService.addComment(
                    requireCurrentUserId(),
                    parseRequiredLong(itemIdField.getText(), "景点ID"),
                    commentField.getText(),
                    Integer.parseInt(ratingField.getText().trim()),
                    List.of("Swing界面"),
                    "127.0.0.1"
            );
            return "评论提交成功";
        }, detailArea::setText));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), wrapWithTitle("景点详情", detailArea));
        splitPane.setResizeWeight(0.62);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOrderPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField userIdField = new JTextField(10);
        JTextField orderIdField = new JTextField(10);
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"0-待支付", "1-已支付", "2-已取消", "3-已完成"});
        DefaultTableModel model = tableModel("订单ID", "用户ID", "景点ID", "金额", "状态", "创建时间");
        JTable table = createTable(model);

        JPanel toolbar = toolbar();
        JButton listButton = new JButton("查询订单");
        JButton updateButton = new JButton("更新状态");
        toolbar.add(new JLabel("用户ID"));
        toolbar.add(userIdField);
        toolbar.add(listButton);
        toolbar.add(new JLabel("订单ID"));
        toolbar.add(orderIdField);
        toolbar.add(statusBox);
        toolbar.add(updateButton);

        table.getSelectionModel().addListSelectionListener(event -> {
            int row = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                orderIdField.setText(String.valueOf(table.getValueAt(row, 0)));
            }
        });

        listButton.addActionListener(event -> runTask("订单查询", () -> businessService.listUserOrders(
                userIdField.getText().isBlank() ? requireCurrentUserId() : parseRequiredLong(userIdField.getText(), "用户ID"),
                50,
                0
        ), orders -> {
            model.setRowCount(0);
            for (Order order : orders) {
                model.addRow(new Object[]{
                        order.getOrderId(), order.getUserId(), order.getItemId(), order.getAmount(),
                        formatOrderStatus(order.getStatus()), order.getCreatedAt()
                });
            }
            setStatus("查询到 " + orders.size() + " 条订单");
        }));

        updateButton.addActionListener(event -> runTask("更新订单状态", () -> businessService.updateOrderStatus(
                parseRequiredLong(orderIdField.getText(), "订单ID"),
                statusBox.getSelectedIndex()
        ), updated -> setStatus(updated ? "订单状态已更新" : "订单状态未变化")));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createManagePanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        DefaultTableModel categoryModel = tableModel("分类ID", "名称", "父分类ID");
        JTable categoryTable = createTable(categoryModel);
        JTextArea resultArea = createTextArea(8, 60);

        JTextField categoryNameField = new JTextField(12);
        JTextField parentIdField = new JTextField(8);
        JTextField itemTitleField = new JTextField(16);
        JTextField itemCategoryField = new JTextField(8);
        JTextField itemDescField = new JTextField(20);
        JTextField itemStatusIdField = new JTextField(8);
        JComboBox<String> itemStatusBox = new JComboBox<>(new String[]{"0-下架", "1-上架"});

        JPanel toolbar = toolbar();
        JButton refreshCategoryButton = new JButton("刷新分类");
        JButton createCategoryButton = new JButton("新增分类");
        JButton createItemButton = new JButton("新增景点");
        JButton itemStatusButton = new JButton("更新景点状态");
        toolbar.add(refreshCategoryButton);
        toolbar.add(new JLabel("分类名"));
        toolbar.add(categoryNameField);
        toolbar.add(new JLabel("父ID"));
        toolbar.add(parentIdField);
        toolbar.add(createCategoryButton);
        toolbar.add(new JLabel("景点标题"));
        toolbar.add(itemTitleField);
        toolbar.add(new JLabel("分类ID"));
        toolbar.add(itemCategoryField);
        toolbar.add(new JLabel("描述"));
        toolbar.add(itemDescField);
        toolbar.add(createItemButton);
        toolbar.add(new JLabel("景点ID"));
        toolbar.add(itemStatusIdField);
        toolbar.add(itemStatusBox);
        toolbar.add(itemStatusButton);

        refreshCategoryButton.addActionListener(event -> refreshCategories(categoryModel));
        createCategoryButton.addActionListener(event -> runAdminTask("新增分类", () -> businessService.createCategory(
                categoryNameField.getText(), parseOptionalLong(parentIdField.getText())
        ), id -> {
            resultArea.setText("分类创建成功，ID：" + id);
            refreshCategories(categoryModel);
        }));

        createItemButton.addActionListener(event -> runAdminTask("新增景点", () -> businessService.createItem(
                itemTitleField.getText(),
                parseRequiredLong(itemCategoryField.getText(), "分类ID"),
                itemDescField.getText(),
                List.of(),
                new Document("source", "Swing后台")
        ), id -> resultArea.setText("景点创建成功，ID：" + id)));

        itemStatusButton.addActionListener(event -> runAdminTask("更新景点状态", () -> businessService.updateItemStatus(
                parseRequiredLong(itemStatusIdField.getText(), "景点ID"),
                itemStatusBox.getSelectedIndex()
        ), updated -> resultArea.setText(updated ? "景点状态已更新" : "景点状态未变化")));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(categoryTable), wrapWithTitle("操作结果", resultArea));
        splitPane.setResizeWeight(0.68);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRecommendPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField userIdField = new JTextField(10);
        DefaultTableModel tableModel = tableModel("景点ID", "标题", "分数", "原因", "评分摘要");
        JTable table = createTable(tableModel);

        JPanel toolbar = toolbar();
        JButton personalButton = new JButton("个性化推荐");
        JButton hotButton = new JButton("热门推荐");
        JButton ratedButton = new JButton("高评分推荐");
        toolbar.add(new JLabel("用户ID"));
        toolbar.add(userIdField);
        toolbar.add(personalButton);
        toolbar.add(hotButton);
        toolbar.add(ratedButton);

        personalButton.addActionListener(event -> runTask("个性化推荐", () -> recommendService.recommendForUser(
                userIdField.getText().isBlank() ? requireCurrentUserId() : parseRequiredLong(userIdField.getText(), "用户ID"), 10
        ), recommendations -> fillRecommendationTable(tableModel, recommendations)));

        hotButton.addActionListener(event -> runTask("热门推荐", () -> recommendService.recommendHotItems(null, null, 10),
                recommendations -> fillRecommendationTable(tableModel, recommendations)));

        ratedButton.addActionListener(event -> runTask("高评分推荐", () -> recommendService.recommendTopRatedItems(10),
                recommendations -> fillRecommendationTable(tableModel, recommendations)));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReportPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField yearField = new JTextField(String.valueOf(LocalDate.now().getYear()), 6);
        JTextField monthField = new JTextField(String.valueOf(LocalDate.now().getMonthValue()), 4);
        JTextField userIdField = new JTextField(10);
        JTextArea reportArea = createTextArea(24, 80);

        JPanel toolbar = toolbar();
        JButton monthlyButton = new JButton("月度订单");
        JButton hotButton = new JButton("热门排行");
        JButton userButton = new JButton("用户报告");
        JButton dashboardButton = new JButton("仪表盘汇总");
        toolbar.add(new JLabel("年份"));
        toolbar.add(yearField);
        toolbar.add(new JLabel("月份"));
        toolbar.add(monthField);
        toolbar.add(monthlyButton);
        toolbar.add(hotButton);
        toolbar.add(new JLabel("用户ID"));
        toolbar.add(userIdField);
        toolbar.add(userButton);
        toolbar.add(dashboardButton);

        monthlyButton.addActionListener(event -> runTask("月度订单报表", () -> statisticsService.getMonthlyOrderReport(
                Integer.parseInt(yearField.getText().trim()),
                Integer.parseInt(monthField.getText().trim())
        ), reports -> reportArea.setText(formatMonthlyReports(reports))));

        hotButton.addActionListener(event -> runTask("热门排行", () -> statisticsService.getHotItemRanking(null, null, 10),
                documents -> reportArea.setText(formatDocuments(documents))));

        userButton.addActionListener(event -> runTask("用户报告", () -> statisticsService.getUserReport(
                userIdField.getText().isBlank() ? requireCurrentUserId() : parseRequiredLong(userIdField.getText(), "用户ID"),
                null,
                null
        ), document -> reportArea.setText(documentToText(document))));

        dashboardButton.addActionListener(event -> runTask("仪表盘汇总", () -> statisticsService.buildDashboardReport(
                null, null, Integer.parseInt(yearField.getText().trim()), Integer.parseInt(monthField.getText().trim())
        ), dto -> reportArea.setText(new StringBuilder()
                .append("热门景点：").append(System.lineSeparator()).append(formatDocuments(dto.getHotItems()))
                .append(System.lineSeparator()).append("行为类型：").append(System.lineSeparator()).append(formatDocuments(dto.getActionTypeSummary()))
                .append(System.lineSeparator()).append("热门标签：").append(System.lineSeparator()).append(formatDocuments(dto.getHotTags()))
                .append(System.lineSeparator()).append("系统审计：").append(System.lineSeparator()).append(formatDocuments(dto.getSystemAuditSummary()))
                .append(System.lineSeparator()).append("月度订单：").append(System.lineSeparator()).append(formatMonthlyReports(dto.getMonthlyOrderReport()))
                .toString())));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(wrapWithTitle("报表结果", reportArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAuditPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField userIdField = new JTextField(10);
        JTextField logTypeField = new JTextField(10);
        JComboBox<String> levelBox = new JComboBox<>(new String[]{"", "INFO", "WARN", "ERROR"});
        JTextArea auditArea = createTextArea(24, 80);

        JPanel toolbar = toolbar();
        JButton queryButton = new JButton("查询日志");
        JButton summaryButton = new JButton("审计汇总");
        JButton trendButton = new JButton("审计趋势");
        JButton userSummaryButton = new JButton("用户操作汇总");
        toolbar.add(new JLabel("用户ID"));
        toolbar.add(userIdField);
        toolbar.add(new JLabel("类型"));
        toolbar.add(logTypeField);
        toolbar.add(new JLabel("级别"));
        toolbar.add(levelBox);
        toolbar.add(queryButton);
        toolbar.add(summaryButton);
        toolbar.add(trendButton);
        toolbar.add(userSummaryButton);

        queryButton.addActionListener(event -> runAdminTask("审计日志查询", () -> systemLogService.queryAuditLogs(
                parseOptionalLong(userIdField.getText()),
                blankToNull(logTypeField.getText()),
                blankToNull((String) levelBox.getSelectedItem()),
                null,
                null,
                80
        ), documents -> auditArea.setText(formatDocuments(documents))));

        summaryButton.addActionListener(event -> runAdminTask("审计汇总", () -> systemLogService.getAuditSummary(null, null),
                documents -> auditArea.setText(formatDocuments(documents))));

        trendButton.addActionListener(event -> runAdminTask("审计趋势", () -> systemLogService.getDailyAuditTrend(null, null),
                documents -> auditArea.setText(formatDocuments(documents))));

        userSummaryButton.addActionListener(event -> runAdminTask("用户操作汇总",
                () -> systemLogService.getUserOperationSummary(null, null, 50),
                documents -> auditArea.setText(formatDocuments(documents))));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(wrapWithTitle("审计结果", auditArea), BorderLayout.CENTER);
        return panel;
    }

    private void setCurrentUser(LoginResult result) {
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
        userLabel.setText("当前用户：" + currentUser.getUsername() + " / " + currentUser.getRole()
                + " / ID " + currentUser.getUserId());
    }

    private void refreshHomeSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("当前进度：项目已接入 Swing 前端页面。").append(System.lineSeparator());
        builder.append("页面范围：登录注册、个人档案、景点浏览、订单管理、后台管理、推荐、统计报表、系统审计。")
                .append(System.lineSeparator());
        builder.append("当前登录：")
                .append(currentUser == null ? "未登录" : currentUser.getUsername() + " / " + currentUser.getRole())
                .append(System.lineSeparator());
        builder.append("后端服务：UserService、BusinessService、RecommendService、StatisticsService、SystemLogService。")
                .append(System.lineSeparator());
        homeSummaryArea.setText(builder.toString());
    }

    private void refreshCategories(DefaultTableModel model) {
        runAdminTask("刷新分类", businessService::listCategories, categories -> {
            model.setRowCount(0);
            for (Category category : categories) {
                model.addRow(new Object[]{category.getCategoryId(), category.getName(), category.getParentId()});
            }
        });
    }

    private JPanel pagePanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.setBackground(BACKGROUND);
        return panel;
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        JLabel label = new JLabel(title);
        label.setFont(SECTION_FONT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel metricCard(String title, java.util.function.Supplier<String> valueSupplier) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(100, 110, 120));
        JLabel valueLabel = new JLabel(valueSupplier.get());
        valueLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private JButton navButton(String text, String tabTitle) {
        JButton button = new JButton(text);
        button.addActionListener(event -> switchTo(tabTitle));
        return button;
    }

    private JPanel toolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        return toolbar;
    }

    private JTextArea createTextArea(int rows, int columns) {
        JTextArea textArea = new JTextArea(rows, columns);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        return textArea;
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
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
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            showError(new IllegalStateException("请先使用管理员账号登录"));
            return;
        }
        runTask(name, task, onSuccess);
    }

    private <T> void runTask(String name, Callable<T> task, Consumer<T> onSuccess) {
        setStatus(name + "处理中...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    onSuccess.accept(result);
                    setStatus(name + "完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setStatus(name + "已中断");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    showError(cause);
                    setStatus(name + "失败：" + cause.getMessage());
                }
            }
        }.execute();
    }

    private void showError(Throwable throwable) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                this,
                throwable.getMessage(),
                "操作失败",
                JOptionPane.ERROR_MESSAGE
        ));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void switchTo(String title) {
        for (int i = 0; i < tabs.getTabCount(); i += 1) {
            if (title.equals(tabs.getTitleAt(i))) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
    }

    private void fillRecommendationTable(DefaultTableModel tableModel, List<RecommendationDTO> recommendations) {
        tableModel.setRowCount(0);
        for (RecommendationDTO recommendation : recommendations) {
            Item item = recommendation.getItem();
            tableModel.addRow(new Object[]{
                    item == null ? "" : item.getItemId(),
                    item == null ? "" : item.getTitle(),
                    recommendation.getScore(),
                    recommendation.getReason(),
                    documentToText(recommendation.getRatingSummary())
            });
        }
    }

    private String formatItemDetail(CrossDatabaseItemDTO dto) {
        StringBuilder builder = new StringBuilder();
        builder.append(dto.getItem().getTitle()).append(System.lineSeparator());
        builder.append("基础信息：ID ").append(dto.getItem().getItemId())
                .append(" / 分类 ").append(dto.getItem().getCategoryId())
                .append(" / 状态 ").append(formatItemStatus(dto.getItem().getStatus()))
                .append(System.lineSeparator());
        builder.append("详情：").append(documentToText(dto.getDetail())).append(System.lineSeparator());
        builder.append("评分：").append(documentToText(dto.getRatingSummary())).append(System.lineSeparator());
        builder.append("评论：").append(System.lineSeparator()).append(formatDocuments(dto.getComments()));
        builder.append("行为摘要：").append(System.lineSeparator()).append(formatDocuments(dto.getBehaviorSummary()));
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

    private String documentToText(Document document) {
        return document == null ? "{}" : document.toJson();
    }

    private String formatItemStatus(Integer status) {
        return status != null && status == 1 ? "上架" : "下架";
    }

    private String formatOrderStatus(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已完成";
            default -> "未知";
        };
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
        return Long.parseLong(value.trim());
    }

    private long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return Long.parseLong(value.trim());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
