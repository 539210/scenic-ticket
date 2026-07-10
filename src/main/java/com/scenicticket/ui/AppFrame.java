package com.scenicticket.ui;

import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.dto.LoginResult;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.dto.StatisticsReportDTO;
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
import javax.swing.SwingWorker;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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

    private final JLabel userLabel = new JLabel("未登录");
    private final JLabel statusLabel = new JLabel("就绪");
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
    private final JTextArea homeSummaryArea = createTextArea(10, 80);
    private final JLabel homeUserValue = new JLabel("未登录");
    private final JLabel homeRoleValue = new JLabel("-");
    private final Map<Long, String> categoryNames = new LinkedHashMap<>();

    private User currentUser;
    private int runningTasks;

    public AppFrame() {
        setTitle("景点售票系统");
        setMinimumSize(new Dimension(1100, 680));
        setSize(1360, 840);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND);
        showLoginView("请输入账号密码登录");
    }

    private void showLoginView(String message) {
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
            showLoginView("已退出登录");
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
        loginPanel.setPreferredSize(new Dimension(420, 220));
        addField(loginPanel, 0, "用户名", loginUsername);
        addField(loginPanel, 1, "密码", loginPassword);
        addFormButtons(loginPanel, 2, loginButton, registerPageButton);
        addFormMessage(loginPanel, 3, loginResult);

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
        addTextAreaField(form, 3, "备注", notesArea);
        addFormButtons(form, 4, refreshButton, saveButton);
        addFormMessage(form, 5, message);

        refreshButton.addActionListener(event -> runTask("刷新档案", () -> userService.getProfile(
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
            return userService.updateProfile(profile);
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
        JTextArea detailArea = createTextArea(18, 34);
        DefaultTableModel tableModel = tableModel("景点名称", "类型", "票价", "折扣", "推荐分", "状态");
        JTable table = createTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setColumnWidths(table, 220, 120, 90, 90, 90, 90);
        detailArea.setText("请选择查询条件，或点击“查询”浏览全部景点。");
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

        JButton detailButton = secondaryButton("查看详情");
        JButton commentsButton = secondaryButton("查看评论");
        JButton orderButton = primaryButton("购买门票");
        JButton commentButton = secondaryButton("发表评论");
        detailButton.setEnabled(false);
        commentsButton.setEnabled(false);
        orderButton.setEnabled(false);
        commentButton.setEnabled(false);
        JPanel detailActions = toolbar();
        detailActions.add(detailButton);
        detailActions.add(commentsButton);
        detailActions.add(orderButton);
        detailActions.add(commentButton);
        JPanel detailPanel = new JPanel(new BorderLayout(0, 10));
        detailPanel.setOpaque(false);
        detailPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        detailPanel.add(detailActions, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(event -> {
            int viewRow = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && viewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                selectedItem[0] = visibleItems.get(modelRow);
                Item item = selectedItem[0];
                String reason = recommendationReasons.get(item.getItemId());
                detailArea.setText(item.getTitle() + System.lineSeparator()
                        + "类型：" + categoryName(item.getCategoryId()) + System.lineSeparator()
                        + "票价：" + UiFormatters.money(item.getPrice()) + "    折扣："
                        + discountText(item.getDiscountRate()) + System.lineSeparator()
                        + "状态：" + formatItemStatus(item.getStatus())
                        + (reason == null ? "" : System.lineSeparator() + "推荐理由：" + reason)
                        + System.lineSeparator() + System.lineSeparator()
                        + "点击“查看详情”可查看开放时间、地址与评论。" );
                detailButton.setEnabled(true);
                commentsButton.setEnabled(true);
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
                        discountText(item.getDiscountRate()), "-", formatItemStatus(item.getStatus())
                });
            }
            resetItemSelection(table, selectedItem, detailButton, commentsButton, orderButton, commentButton);
            detailArea.setText(items.isEmpty() ? "没有找到符合条件的景点，请调整查询条件。" : "共找到 " + items.size() + " 个景点，请从左侧列表选择。" );
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
                        discountText(item.getDiscountRate()), formatRecommendationScore(recommendation.getScore()),
                        formatItemStatus(item.getStatus())
                });
            }
            resetItemSelection(table, selectedItem, detailButton, commentsButton, orderButton, commentButton);
            detailArea.setText(recommendations.isEmpty() ? "暂时没有推荐结果。" : "已生成 " + recommendations.size() + " 个推荐结果，请选择景点查看推荐理由。" );
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

        detailButton.addActionListener(event -> runTask("景点详情", () -> crossDatabaseQueryService.getItemDetail(
                requireSelectedItem(selectedItem).getItemId(), 8
        ), dto -> detailArea.setText(formatItemDetail(dto))));

        commentsButton.addActionListener(event -> runTask("查看评论", () -> crossDatabaseQueryService.getItemDetail(
                requireSelectedItem(selectedItem).getItemId(), 20
        ), dto -> detailArea.setText(formatItemCommentsView(dto))));

        orderButton.addActionListener(event -> showPurchaseDialog(requireSelectedItem(selectedItem), detailArea));
        commentButton.addActionListener(event -> showCommentDialog(requireSelectedItem(selectedItem), detailArea));

        keywordField.addActionListener(event -> searchButton.doClick());
        loadCategoryChoices(categoryBox, true);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrapWithTitle("景点列表", new JScrollPane(table)),
                wrapWithTitle("景点详情与评论", detailPanel));
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
        JComboBox<String> updateStatusBox = new JComboBox<>(new String[]{"0-待支付", "1-已支付", "2-已取消", "3-已完成"});
        DefaultTableModel model = isCurrentAdmin()
                ? tableModel("订单号", "用户ID", "景点名称", "票数", "单价", "折扣", "实付金额", "付款方式", "状态", "创建时间")
                : tableModel("订单号", "景点名称", "票数", "单价", "折扣", "实付金额", "付款方式", "状态", "创建时间");
        JTable table = createTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        if (isCurrentAdmin()) {
            setColumnWidths(table, 80, 80, 190, 60, 90, 80, 100, 90, 90, 170);
        } else {
            setColumnWidths(table, 80, 210, 60, 90, 80, 100, 90, 90, 170);
        }
        Long[] selectedOrderId = new Long[1];

        JPanel queryToolbar = toolbar();
        JButton listButton = primaryButton("查询订单");
        JButton refreshButton = secondaryButton("刷新");
        JButton resetButton = secondaryButton("重置");
        JButton updateButton = primaryButton("更新状态");
        updateButton.setEnabled(false);
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

        JPanel controls = new JPanel(new GridLayout(isCurrentAdmin() ? 2 : 1, 1, 0, 8));
        controls.setOpaque(false);
        controls.add(wrapWithTitle("订单查询", queryToolbar));
        if (isCurrentAdmin()) {
            JPanel updateToolbar = toolbar();
            JLabel selectedOrderLabel = new JLabel("请先从表格选择订单");
            updateToolbar.add(selectedOrderLabel);
            updateToolbar.add(new JLabel("状态改为"));
            updateToolbar.add(updateStatusBox);
            updateToolbar.add(updateButton);
            controls.add(wrapWithTitle("订单状态更新", updateToolbar));
            table.getSelectionModel().addListSelectionListener(event -> {
                int row = table.getSelectedRow();
                if (!event.getValueIsAdjusting() && row >= 0) {
                    selectedOrderId[0] = Long.valueOf(String.valueOf(table.getValueAt(row, 0)));
                    selectedOrderLabel.setText("已选择订单：" + selectedOrderId[0]);
                    updateButton.setEnabled(true);
                }
            });
        }

        Runnable refreshOrders = () -> runTask("订单查询", () -> {
            Long queryUserId = isCurrentAdmin()
                    ? parseOptionalLong(userIdField.getText())
                    : Long.valueOf(requireCurrentUserId());
            return businessService.searchOrderViews(
                    queryUserId,
                    parseOptionalLong(queryOrderIdField.getText()),
                    selectedOrderStatus(queryStatusBox),
                    50,
                    0
            );
        }, orderViews -> {
            model.setRowCount(0);
            selectedOrderId[0] = null;
            updateButton.setEnabled(false);
            for (OrderViewDTO view : orderViews) {
                Order order = view.getOrder();
                if (isCurrentAdmin()) {
                    model.addRow(new Object[]{order.getOrderId(), order.getUserId(), view.getItemTitle(), order.getQuantity(),
                            UiFormatters.money(order.getUnitPrice()), discountText(order.getDiscountRate()),
                            UiFormatters.money(order.getAmount()), order.getPaymentMethod(),
                            formatOrderStatus(order.getStatus()), formatDate(order.getCreatedAt())});
                } else {
                    model.addRow(new Object[]{order.getOrderId(), view.getItemTitle(), order.getQuantity(),
                            UiFormatters.money(order.getUnitPrice()), discountText(order.getDiscountRate()),
                            UiFormatters.money(order.getAmount()), order.getPaymentMethod(),
                            formatOrderStatus(order.getStatus()), formatDate(order.getCreatedAt())});
                }
            }
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

        updateButton.addActionListener(event -> runTask("更新订单状态", () -> businessService.updateOrderStatus(
                requireSelectedOrderId(selectedOrderId),
                updateStatusBox.getSelectedIndex()
        ), updated -> {
            setStatus(updated ? "订单状态已更新" : "订单状态未变化");
            refreshOrders.run();
        }));

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
        DefaultTableModel itemModel = tableModel("ID", "景点名称", "类型", "票价", "折扣", "状态", "更新时间");
        JTable itemTable = createTable(itemModel);
        itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setColumnWidths(itemTable, 60, 230, 130, 90, 80, 80, 170);
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
        JTextField itemPriceField = new JTextField(8);
        JTextField itemDiscountField = new JTextField(6);
        JComboBox<String> itemStatusBox = new JComboBox<>(new String[]{"下架", "上架"});
        JButton pricingButton = primaryButton("保存价格折扣");
        JButton itemStatusButton = secondaryButton("更新上下架");
        pricingButton.setEnabled(false);
        itemStatusButton.setEnabled(false);
        JPanel itemEditToolbar = toolbar();
        itemEditToolbar.add(selectedItemLabel);
        itemEditToolbar.add(new JLabel("票价"));
        itemEditToolbar.add(itemPriceField);
        itemEditToolbar.add(new JLabel("折扣%"));
        itemEditToolbar.add(itemDiscountField);
        itemEditToolbar.add(pricingButton);
        itemEditToolbar.add(new JLabel("状态"));
        itemEditToolbar.add(itemStatusBox);
        itemEditToolbar.add(itemStatusButton);

        Runnable refreshItems = () -> runAdminTask("刷新景点", () -> businessService.searchAllItemsForAdmin(
                itemKeywordField.getText(), selectedCategoryId(itemCategoryBox), 100, 0
        ), items -> {
            itemModel.setRowCount(0);
            adminItems.clear();
            adminItems.addAll(items);
            for (Item item : items) {
                itemModel.addRow(new Object[]{item.getItemId(), item.getTitle(), categoryName(item.getCategoryId()),
                        UiFormatters.money(item.getPrice()), discountText(item.getDiscountRate()),
                        formatItemStatus(item.getStatus()), formatDate(item.getUpdatedAt())});
            }
            selectedItem[0] = null;
            selectedItemLabel.setText("请先从表格选择景点");
            pricingButton.setEnabled(false);
            itemStatusButton.setEnabled(false);
            setStatus("已加载 " + items.size() + " 个景点");
        });

        itemTable.getSelectionModel().addListSelectionListener(event -> {
            int row = itemTable.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                selectedItem[0] = adminItems.get(itemTable.convertRowIndexToModel(row));
                Item item = selectedItem[0];
                selectedItemLabel.setText("已选择：" + item.getTitle());
                itemPriceField.setText(item.getPrice() == null ? "0.00" : item.getPrice().toPlainString());
                itemDiscountField.setText(item.getDiscountRate() == null ? "0" : item.getDiscountRate().stripTrailingZeros().toPlainString());
                itemStatusBox.setSelectedIndex(item.getStatus() != null && item.getStatus() == 1 ? 1 : 0);
                pricingButton.setEnabled(true);
                itemStatusButton.setEnabled(true);
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
        pricingButton.addActionListener(event -> runAdminTask("保存价格折扣", () -> businessService.updateItemPricing(
                requireSelectedItem(selectedItem).getItemId(), parseRequiredAmount(itemPriceField.getText(), "票价"),
                parseRequiredAmount(itemDiscountField.getText(), "折扣")
        ), updated -> {
            setStatus(updated ? "景点价格和折扣已保存" : "价格和折扣没有变化");
            refreshItems.run();
        }));
        itemStatusButton.addActionListener(event -> runAdminTask("更新上下架状态", () -> businessService.updateItemStatus(
                requireSelectedItem(selectedItem).getItemId(), itemStatusBox.getSelectedIndex()
        ), updated -> {
            setStatus(updated ? "景点上下架状态已更新" : "景点状态没有变化");
            refreshItems.run();
        }));

        JPanel itemTop = new JPanel(new GridLayout(2, 1, 0, 8));
        itemTop.setOpaque(false);
        itemTop.add(wrapWithTitle("筛选景点", itemQueryToolbar));
        itemTop.add(wrapWithTitle("编辑所选景点", itemEditToolbar));
        itemPage.add(itemTop, BorderLayout.NORTH);
        itemPage.add(wrapWithTitle("景点列表", new JScrollPane(itemTable)), BorderLayout.CENTER);

        JPanel categoryPage = new JPanel(new BorderLayout(12, 12));
        categoryPage.setOpaque(false);
        DefaultTableModel categoryModel = tableModel("分类ID", "分类名称", "上级分类");
        JTable categoryTable = createTable(categoryModel);
        categoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JTextField categoryNameField = new JTextField(16);
        JComboBox<CategoryOption> parentCategoryBox = new JComboBox<>();
        parentCategoryBox.addItem(new CategoryOption("无上级分类", null));
        JButton createCategoryButton = primaryButton("新增分类");
        JButton refreshCategoryButton = secondaryButton("刷新");
        JPanel categoryToolbar = toolbar();
        categoryToolbar.add(new JLabel("分类名称"));
        categoryToolbar.add(categoryNameField);
        categoryToolbar.add(new JLabel("上级分类"));
        categoryToolbar.add(parentCategoryBox);
        categoryToolbar.add(createCategoryButton);
        categoryToolbar.add(refreshCategoryButton);

        Runnable refreshCategories = () -> runAdminTask("刷新分类", businessService::listCategories, categories -> {
            updateCategoryCache(categories);
            fillCategoryCombo(itemCategoryBox, categories, true, "全部类型");
            fillCategoryCombo(parentCategoryBox, categories, true, "无上级分类");
            categoryModel.setRowCount(0);
            for (Category category : categories) {
                categoryModel.addRow(new Object[]{category.getCategoryId(), category.getName(),
                        category.getParentId() == null ? "-" : categoryName(category.getParentId())});
            }
            setStatus("已加载 " + categories.size() + " 个分类");
            refreshItems.run();
        });
        refreshCategoryButton.addActionListener(event -> refreshCategories.run());
        createCategoryButton.addActionListener(event -> runAdminTask("新增分类", () -> businessService.createCategory(
                categoryNameField.getText(), selectedCategoryId(parentCategoryBox)
        ), id -> {
            categoryNameField.setText("");
            setStatus("分类创建成功，编号：" + id);
            refreshCategories.run();
        }));
        categoryNameField.addActionListener(event -> createCategoryButton.doClick());
        categoryPage.add(wrapWithTitle("新增分类", categoryToolbar), BorderLayout.NORTH);
        categoryPage.add(wrapWithTitle("分类列表", new JScrollPane(categoryTable)), BorderLayout.CENTER);

        manageTabs.addTab("景点管理", itemPage);
        manageTabs.addTab("分类管理", categoryPage);
        panel.add(manageTabs, BorderLayout.CENTER);
        refreshCategories.run();
        return panel;
    }

    private JPanel createReportPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField yearField = new JTextField(String.valueOf(LocalDate.now().getYear()), 6);
        JTextField monthField = new JTextField(String.valueOf(LocalDate.now().getMonthValue()), 4);
        JTextField userIdField = new JTextField(10);
        DefaultTableModel monthlyModel = tableModel("日期", "订单数", "销售金额");
        DefaultTableModel hotModel = tableModel("排名", "景点ID", "总操作", "浏览", "下单", "平均停留(秒)");
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
            for (Document document : documents) {
                hotModel.addRow(new Object[]{rank, valueText(document.get("_id")), numberText(document.get("total_actions")),
                        numberText(document.get("view_count")), numberText(document.get("order_count")),
                        decimalText(document.get("avg_duration"))});
                rank += 1;
            }
            resultTabs.setSelectedIndex(1);
            setStatus("热门排行已更新");
        });
        hotButton.addActionListener(event -> loadHot.run());

        Runnable loadUserReport = () -> runTask("用户报告", () -> statisticsService.getUserReport(
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
                null, null, parseRequiredInt(yearField.getText(), "年份"), parseRequiredInt(monthField.getText(), "月份")
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
        JPanel auditActions = toolbar();
        JPanel auditToolbar = new JPanel(new GridLayout(2, 1, 0, 4));
        auditToolbar.setOpaque(false);
        JButton queryButton = primaryButton("查询日志");
        JButton summaryButton = secondaryButton("审计汇总");
        JButton trendButton = secondaryButton("审计趋势");
        JButton userSummaryButton = secondaryButton("用户操作");
        JButton refreshButton = secondaryButton("刷新当前结果");
        auditFilters.add(new JLabel("用户ID"));
        auditFilters.add(userIdField);
        auditFilters.add(new JLabel("类型"));
        auditFilters.add(logTypeBox);
        auditFilters.add(new JLabel("级别"));
        auditFilters.add(levelBox);
        auditActions.add(queryButton);
        auditActions.add(summaryButton);
        auditActions.add(trendButton);
        auditActions.add(userSummaryButton);
        auditActions.add(refreshButton);
        auditToolbar.add(auditFilters);
        auditToolbar.add(auditActions);

        Runnable refreshAuditLogs = () -> runAdminTask("审计日志查询", () -> systemLogService.queryAuditLogs(
                parseOptionalLong(userIdField.getText()),
                selectedLogType(logTypeBox),
                selectedLogLevel(levelBox),
                null,
                null,
                80
        ), documents -> {
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

        Runnable loadSummary = () -> runAdminTask("审计汇总", () -> systemLogService.getAuditSummary(null, null), documents -> {
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

        Runnable loadTrend = () -> runAdminTask("审计趋势", () -> systemLogService.getDailyAuditTrend(null, null), documents -> {
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
                () -> systemLogService.getUserOperationSummary(null, null, 50), documents -> {
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

        panel.add(wrapWithTitle("审计条件", auditToolbar), BorderLayout.NORTH);
        panel.add(wrapWithTitle("审计结果", auditTabs), BorderLayout.CENTER);
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
        JPanel form = formPanel("新增景点");
        addField(form, 0, "景点名称", titleField);
        addField(form, 1, "景点类型", categoryBox);
        addTextAreaField(form, 2, "景点描述", descriptionArea);
        addField(form, 3, "固定票价", priceField);
        addField(form, 4, "折扣%", discountField);
        int result = JOptionPane.showConfirmDialog(this, form, "新增景点",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runAdminTask("新增景点", () -> businessService.createItem(titleField.getText(), requireCategoryId(categoryBox),
                descriptionArea.getText(), List.of(), new Document("source", "Swing后台"),
                parseRequiredAmount(priceField.getText(), "票价"), parseRequiredAmount(discountField.getText(), "折扣")), id -> {
            setStatus("景点创建成功，编号：" + id);
            refreshItems.run();
        });
    }

    private void showPurchaseDialog(Item item, JTextArea detailArea) {
        if (item.getStatus() == null || item.getStatus() != 1) {
            showError(new IllegalArgumentException("该景点当前未上架，暂不能购买"));
            return;
        }
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        JComboBox<String> paymentBox = new JComboBox<>(new String[]{"微信", "支付宝", "银行卡", "现金"});
        JLabel amountLabel = new JLabel();
        amountLabel.setFont(SECTION_FONT);
        Runnable updateAmount = () -> amountLabel.setText(UiFormatters.money(UiFormatters.orderAmount(
                item.getPrice(), item.getDiscountRate(), (Integer) quantitySpinner.getValue())));
        quantitySpinner.addChangeListener(event -> updateAmount.run());
        updateAmount.run();
        JPanel form = formPanel("确认购买");
        addField(form, 0, "景点", new JLabel(item.getTitle()));
        addField(form, 1, "单价", new JLabel(UiFormatters.money(item.getPrice())));
        addField(form, 2, "折扣", new JLabel(discountText(item.getDiscountRate())));
        addField(form, 3, "购买票数", quantitySpinner);
        addField(form, 4, "付款方式", paymentBox);
        addField(form, 5, "实付金额", amountLabel);
        int result = JOptionPane.showConfirmDialog(this, form, "购买门票",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runTask("购买门票", () -> businessService.createOrder(requireCurrentUserId(), item.getItemId(),
                (Integer) quantitySpinner.getValue(), (String) paymentBox.getSelectedItem()), orderId ->
                detailArea.setText("付款成功" + System.lineSeparator()
                        + "景点：" + item.getTitle() + System.lineSeparator()
                        + "订单号：" + orderId + System.lineSeparator()
                        + "实付金额：" + amountLabel.getText() + System.lineSeparator()
                        + "现在可以点击“发表评论”分享体验。"));
    }

    private void showCommentDialog(Item item, JTextArea detailArea) {
        JSpinner ratingSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        JTextArea commentArea = new JTextArea(5, 28);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        JPanel form = formPanel("发表评论");
        addField(form, 0, "景点", new JLabel(item.getTitle()));
        addField(form, 1, "评分", ratingSpinner);
        addTextAreaField(form, 2, "评论内容", commentArea);
        int result = JOptionPane.showConfirmDialog(this, form, "发表评论",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runTask("发表评论", () -> {
            if (!businessService.canComment(requireCurrentUserId(), item.getItemId())) {
                throw new IllegalArgumentException("购买成功后才能评论该景点，请先购买门票");
            }
            String comment = commentArea.getText().trim();
            if (comment.isBlank()) {
                throw new IllegalArgumentException("评论内容不能为空");
            }
            behaviorLogService.addComment(requireCurrentUserId(), item.getItemId(), comment,
                    (Integer) ratingSpinner.getValue(), List.of("Swing界面"), "127.0.0.1");
            return "评论提交成功";
        }, message -> detailArea.setText(message + "。点击“查看评论”可查看最新内容。"));
    }

    private Item requireSelectedItem(Item[] selectedItem) {
        if (selectedItem == null || selectedItem.length == 0 || selectedItem[0] == null) {
            throw new IllegalArgumentException("请先从表格中选择一个景点");
        }
        return selectedItem[0];
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
        for (Document document : dto.getHotItems()) {
            model.addRow(new Object[]{"热门景点", "景点ID " + valueText(document.get("_id")),
                    "总操作 " + numberText(document.get("total_actions")) + "，浏览 "
                            + numberText(document.get("view_count")) + "，下单 " + numberText(document.get("order_count"))});
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
        runTask(name, task, onSuccess, null);
    }

    private <T> void runTask(String name, Callable<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
        String processingStatus = name + "处理中...";
        setStatus(processingStatus);
        setBusy(true);
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
                    if (processingStatus.equals(statusLabel.getText())) {
                        setStatus(name + "完成");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setStatus(name + "已中断");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    String message = UiFormatters.chineseError(cause);
                    if (onError == null) {
                        showError(cause);
                    } else {
                        onError.accept(message);
                    }
                    setStatus(name + "失败：" + message);
                } catch (RuntimeException e) {
                    String message = UiFormatters.chineseError(e);
                    if (onError == null) {
                        showError(e);
                    } else {
                        onError.accept(message);
                    }
                    setStatus(name + "失败：" + message);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        runningTasks = Math.max(0, runningTasks + (busy ? 1 : -1));
        boolean active = runningTasks > 0;
        getGlassPane().setVisible(active);
        setCursor(active ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
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

    private String formatItemDetail(CrossDatabaseItemDTO dto) {
        StringBuilder builder = new StringBuilder();
        builder.append(dto.getItem().getTitle()).append(System.lineSeparator());
        builder.append("基础信息：ID ").append(dto.getItem().getItemId())
                .append(" / 类型 ").append(categoryName(dto.getItem().getCategoryId()))
                .append(" / 票价 ").append(dto.getItem().getPrice())
                .append(" / 折扣 ").append(discountText(dto.getItem().getDiscountRate()))
                .append(" / 状态 ").append(formatItemStatus(dto.getItem().getStatus()))
                .append(System.lineSeparator());
        builder.append("详情：").append(formatItemDetailDocument(dto.getDetail())).append(System.lineSeparator());
        builder.append("评分：").append(formatRatingSummary(dto.getRatingSummary())).append(System.lineSeparator());
        builder.append("评论：").append(System.lineSeparator()).append(formatComments(dto.getComments()));
        builder.append("行为摘要：").append(System.lineSeparator()).append(formatBehaviorSummary(dto.getBehaviorSummary()));
        return builder.toString();
    }

    private String formatItemCommentsView(CrossDatabaseItemDTO dto) {
        StringBuilder builder = new StringBuilder();
        builder.append("评论查看").append(System.lineSeparator());
        builder.append("景点：").append(dto.getItem().getTitle())
                .append("（ID ").append(dto.getItem().getItemId()).append("）")
                .append(System.lineSeparator());
        builder.append("评分概览：").append(formatRatingSummary(dto.getRatingSummary()))
                .append(System.lineSeparator());
        builder.append("评论列表：").append(System.lineSeparator())
                .append(formatComments(dto.getComments()));
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
            return "暂无详情";
        }
        Document metadata = detail.get("metadata", Document.class);
        StringBuilder builder = new StringBuilder();
        builder.append(valueText(detail.get("description")));
        if (metadata != null && !metadata.isEmpty()) {
            builder.append(System.lineSeparator())
                    .append("开放时间：").append(valueText(metadata.get("open_time")))
                    .append("  地址：").append(valueText(metadata.get("address")));
        }
        Object images = detail.get("images");
        if (images instanceof List<?> imageList && !imageList.isEmpty()) {
            builder.append(System.lineSeparator()).append("图片数量：").append(imageList.size());
        }
        return builder.toString();
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
            builder.append(index).append(". 用户ID：").append(valueText(document.get("user_id")))
                    .append("  评分：").append(numberText(document.get("rating")))
                    .append("  时间：").append(formatDate(document.get("created_at")))
                    .append(System.lineSeparator())
                    .append("   内容：").append(valueText(document.get("content")))
                    .append(System.lineSeparator())
                    .append("   标签：").append(listText(document.get("tags")))
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

    private String formatHotItems(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无热门景点数据";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            builder.append(index).append(". 景点ID：").append(valueText(document.get("_id")))
                    .append("  总操作：").append(numberText(document.get("total_actions")))
                    .append("  浏览：").append(numberText(document.get("view_count")))
                    .append("  下单：").append(numberText(document.get("order_count")))
                    .append("  平均停留：").append(decimalText(document.get("avg_duration"))).append(" 秒")
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
