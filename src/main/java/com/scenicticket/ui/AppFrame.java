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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ALL_OPTION = "全部";
    private static final String[] KEYWORD_OPTIONS = {
            ALL_OPTION, "南山", "云岭", "青河", "古城", "海湾", "星湖", "观景", "博物馆", "亲子", "水上", "森林"
    };
    private static final CategoryChoice[] CATEGORY_OPTIONS = {
            new CategoryChoice("全部类型", null),
            new CategoryChoice("自然景观", 1L),
            new CategoryChoice("历史文化", 2L),
            new CategoryChoice("主题乐园", 3L),
            new CategoryChoice("山水风光", 4L),
            new CategoryChoice("森林公园", 5L),
            new CategoryChoice("古镇古街", 6L),
            new CategoryChoice("博物展馆", 7L),
            new CategoryChoice("亲子乐园", 8L),
            new CategoryChoice("水上乐园", 9L),
            new CategoryChoice("城市观光", 10L)
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

    private User currentUser;

    public AppFrame() {
        setTitle("景点售票系统");
        setMinimumSize(new Dimension(1180, 760));
        setSize(1280, 820);
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
        header.setBorder(BorderFactory.createEmptyBorder(26, 32, 18, 32));
        header.setBackground(Color.WHITE);

        JLabel title = new JLabel("景点售票系统");
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 26));
        JLabel subtitle = new JLabel("请先登录后进入系统");
        subtitle.setForeground(new Color(100, 110, 120));

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
            showLoginView("已退出登录");
        });
        sessionBlock.add(userLabel);
        sessionBlock.add(logoutButton);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(sessionBlock, BorderLayout.EAST);
        return header;
    }

    private Component createPages() {
        tabs.addTab("首页", scrollPage(createHomePanel()));
        tabs.addTab("个人档案", scrollPage(createProfilePanel()));
        tabs.addTab("景点浏览", scrollPage(createItemPanel()));
        tabs.addTab("我的订单", scrollPage(createOrderPanel()));
        tabs.addTab("统计报表", scrollPage(createReportPanel()));
        if (isCurrentAdmin()) {
            tabs.addTab("后台管理", scrollPage(createManagePanel()));
            tabs.addTab("系统审计", scrollPage(createAuditPanel()));
        }
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

        JPanel metrics = new JPanel(new GridLayout(1, 2, 12, 12));
        metrics.setOpaque(false);
        metrics.add(metricCard("当前账号", homeUserValue));
        metrics.add(metricCard("账号类型", homeRoleValue));

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickActions.setOpaque(false);
        quickActions.add(navButton("个人档案", "个人档案"));
        quickActions.add(navButton("浏览景点", "景点浏览"));
        quickActions.add(navButton("查看订单", "我的订单"));
        quickActions.add(navButton("统计报表", "统计报表"));
        JButton refreshButton = new JButton("刷新首页");
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
        panel.add(wrapWithTitle("系统概览", homeSummaryArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLoginPanel() {
        JTextField loginUsername = new JTextField(22);
        JPasswordField loginPassword = new JPasswordField(22);
        JButton loginButton = new JButton("登录");
        JButton registerPageButton = new JButton("注册新账号");
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
                setStatus(result.getMessage());
            }
        }));

        registerPageButton.addActionListener(event -> showRegisterView());

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
        JButton registerButton = new JButton("注册");
        JButton backButton = new JButton("返回登录");
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
        }));

        backButton.addActionListener(event -> showLoginView("请输入账号密码登录"));

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
        JButton refreshButton = new JButton("刷新档案");
        JButton saveButton = new JButton("保存档案");
        JLabel message = new JLabel(" ");

        JPanel form = formPanel("个人档案");
        addField(form, 0, "用户ID", userIdField);
        addField(form, 1, "真实姓名", realNameField);
        addField(form, 2, "证件号", idCardField);
        addField(form, 3, "地址", addressField);
        addTextAreaField(form, 4, "备注", notesArea);
        addFormButtons(form, 5, refreshButton, saveButton);
        addFormMessage(form, 6, message);

        refreshButton.addActionListener(event -> runTask("刷新档案", () -> userService.getProfile(
                userIdField.getText().isBlank() ? requireCurrentUserId() : parseRequiredLong(userIdField.getText(), "用户ID")
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

        JComboBox<String> keywordBox = new JComboBox<>(KEYWORD_OPTIONS);
        JTextField keywordField = new JTextField(14);
        JComboBox<CategoryChoice> categoryBox = new JComboBox<>(CATEGORY_OPTIONS);
        JTextField itemIdField = new JTextField(8);
        JTextField ticketCountField = new JTextField("1", 4);
        JComboBox<String> paymentBox = new JComboBox<>(new String[]{"微信", "支付宝", "银行卡", "现金"});
        JTextField ratingField = new JTextField("5", 4);
        JTextField commentField = new JTextField(24);
        JTextArea detailArea = createTextArea(14, 70);
        DefaultTableModel tableModel = tableModel("ID", "标题", "类型", "票价", "折扣", "状态", "推荐分", "推荐理由", "更新时间");
        JTable table = createTable(tableModel);
        setColumnWidths(table, 80, 240, 120, 90, 90, 90, 90, 260, 180);
        detailArea.setText("先选择预设关键词或景点类型查询。选中景点后可以查看详情、购买门票；购买成功后可在详情评论区发表评论。");

        JPanel searchToolbar = toolbar();
        JButton searchButton = new JButton("查询景点");
        JButton recommendButton = new JButton("推荐");
        JPopupMenu recommendMenu = new JPopupMenu();
        JMenuItem personalRecommendItem = new JMenuItem("为你推荐");
        JMenuItem ratedRecommendItem = new JMenuItem("高分");
        JMenuItem hotRecommendItem = new JMenuItem("热门");
        recommendMenu.add(personalRecommendItem);
        recommendMenu.add(ratedRecommendItem);
        recommendMenu.add(hotRecommendItem);
        JButton allButton = new JButton("查询全部");
        JButton refreshButton = new JButton("刷新列表");
        JButton clearButton = new JButton("清空条件");
        searchToolbar.add(new JLabel("预设关键词"));
        searchToolbar.add(keywordBox);
        searchToolbar.add(new JLabel("补充关键词"));
        searchToolbar.add(keywordField);
        searchToolbar.add(new JLabel("景点类型"));
        searchToolbar.add(categoryBox);
        searchToolbar.add(searchButton);
        searchToolbar.add(recommendButton);
        searchToolbar.add(allButton);
        searchToolbar.add(refreshButton);
        searchToolbar.add(clearButton);

        JPanel purchaseToolbar = toolbar();
        JButton detailButton = new JButton("查看详情");
        JButton commentsButton = new JButton("查看评论");
        JButton orderButton = new JButton("创建订单");
        purchaseToolbar.add(new JLabel("景点ID（可点表格自动填写）"));
        purchaseToolbar.add(itemIdField);
        purchaseToolbar.add(detailButton);
        purchaseToolbar.add(commentsButton);
        purchaseToolbar.add(new JLabel("购买票数"));
        purchaseToolbar.add(ticketCountField);
        purchaseToolbar.add(new JLabel("付款方式"));
        purchaseToolbar.add(paymentBox);
        purchaseToolbar.add(orderButton);

        JPanel commentToolbar = toolbar();
        JButton commentButton = new JButton("发表评论");
        commentToolbar.add(new JLabel("评分1-5"));
        commentToolbar.add(ratingField);
        commentToolbar.add(new JLabel("评论内容"));
        commentToolbar.add(commentField);
        commentToolbar.add(commentButton);

        JPanel controls = new JPanel(new GridLayout(3, 1, 0, 8));
        controls.setOpaque(false);
        controls.add(wrapWithTitle("景点查询", searchToolbar));
        controls.add(wrapWithTitle("购票", purchaseToolbar));
        controls.add(wrapWithTitle("详情页评论（购买后可评论）", commentToolbar));

        table.getSelectionModel().addListSelectionListener(event -> {
            int row = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                Object value = table.getValueAt(row, 0);
                itemIdField.setText(String.valueOf(value));
            }
        });

        Consumer<List<Item>> fillItems = items -> {
            tableModel.setRowCount(0);
            for (Item item : items) {
                tableModel.addRow(new Object[]{
                        item.getItemId(), item.getTitle(), categoryName(item.getCategoryId()), item.getPrice(),
                        discountText(item.getDiscountRate()), formatItemStatus(item.getStatus()), "", "",
                        formatDate(item.getUpdatedAt())
                });
            }
            setStatus("查询到 " + items.size() + " 个景点。点击表格中的一行即可选择景点。");
        };

        Consumer<List<RecommendationDTO>> fillRecommendations = recommendations -> {
            tableModel.setRowCount(0);
            for (RecommendationDTO recommendation : recommendations) {
                Item item = recommendation.getItem();
                if (item == null) {
                    continue;
                }
                tableModel.addRow(new Object[]{
                        item.getItemId(), item.getTitle(), categoryName(item.getCategoryId()), item.getPrice(),
                        discountText(item.getDiscountRate()), formatItemStatus(item.getStatus()),
                        formatRecommendationScore(recommendation.getScore()), recommendation.getReason(),
                        formatDate(item.getUpdatedAt())
                });
            }
            setStatus("为你找到 " + recommendations.size() + " 个推荐景点。点击表格中的一行即可选择景点。");
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

        refreshButton.addActionListener(event -> runTask("刷新景点列表", () -> businessService.searchItems(
                buildSearchKeyword((String) keywordBox.getSelectedItem(), keywordField.getText()),
                selectedCategoryId(categoryBox), 50, 0
        ), fillItems));

        allButton.addActionListener(event -> {
            keywordBox.setSelectedItem(ALL_OPTION);
            keywordField.setText("");
            categoryBox.setSelectedIndex(0);
            runTask("查询全部景点", () -> businessService.searchItems(null, null, 50, 0), fillItems);
        });

        clearButton.addActionListener(event -> {
            keywordBox.setSelectedItem(ALL_OPTION);
            keywordField.setText("");
            categoryBox.setSelectedIndex(0);
            itemIdField.setText("");
            commentField.setText("");
            tableModel.setRowCount(0);
            detailArea.setText("已清空条件。点击“查询全部”可以重新列出景点。");
            setStatus("景点查询条件已清空");
        });

        detailButton.addActionListener(event -> runTask("景点详情", () -> crossDatabaseQueryService.getItemDetail(
                selectedOrTypedItemId(table, itemIdField), 8
        ), dto -> detailArea.setText(formatItemDetail(dto))));

        commentsButton.addActionListener(event -> runTask("查看评论", () -> crossDatabaseQueryService.getItemDetail(
                selectedOrTypedItemId(table, itemIdField), 20
        ), dto -> detailArea.setText(formatItemCommentsView(dto))));

        orderButton.addActionListener(event -> runTask("创建订单", () -> businessService.createOrder(
                requireCurrentUserId(),
                selectedOrTypedItemId(table, itemIdField),
                parseRequiredInt(ticketCountField.getText(), "购买票数"),
                (String) paymentBox.getSelectedItem()
        ), orderId -> detailArea.setText("模拟付款成功，订单ID：" + orderId + "。现在可以在本页面下方发表评论。")));

        commentButton.addActionListener(event -> runTask("发表评论", () -> {
            long itemId = selectedOrTypedItemId(table, itemIdField);
            if (!businessService.canComment(requireCurrentUserId(), itemId)) {
                throw new IllegalArgumentException("购买成功后才能评论该景点，请先创建订单并完成模拟付款");
            }
            String comment = commentField.getText().trim();
            int rating = parseRequiredInt(ratingField.getText(), "评分");
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("评分必须是 1 到 5 之间的整数");
            }
            if (comment.isBlank()) {
                throw new IllegalArgumentException("评论内容不能为空");
            }
            behaviorLogService.addComment(
                    requireCurrentUserId(),
                    itemId,
                    comment,
                    rating,
                    List.of("Swing界面"),
                    "127.0.0.1"
            );
            return "评论提交成功";
        }, detailArea::setText));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), wrapWithTitle("景点详情与评论", detailArea));
        splitPane.setResizeWeight(0.55);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOrderPanel() {
        JPanel panel = pagePanel(new BorderLayout(12, 12));
        JTextField userIdField = new JTextField(10);
        JTextField queryOrderIdField = new JTextField(10);
        JTextField updateOrderIdField = new JTextField(10);
        JComboBox<String> queryStatusBox = new JComboBox<>(new String[]{"全部状态", "0-待支付", "1-已支付", "2-已取消", "3-已完成"});
        JComboBox<String> updateStatusBox = new JComboBox<>(new String[]{"0-待支付", "1-已支付", "2-已取消", "3-已完成"});
        DefaultTableModel model = tableModel("订单ID", "用户ID", "景点ID", "票数", "单价", "折扣", "实付金额", "付款方式", "状态", "创建时间");
        JTable table = createTable(model);
        setColumnWidths(table, 90, 90, 90, 70, 90, 90, 110, 110, 100, 180);

        JPanel queryToolbar = toolbar();
        JButton listButton = new JButton("查我的订单");
        JButton refreshButton = new JButton("刷新订单");
        JButton updateButton = new JButton("更新状态");
        if (isCurrentAdmin()) {
            queryToolbar.add(new JLabel("用户ID（可不填）"));
            queryToolbar.add(userIdField);
            listButton.setText("查询订单");
        }
        queryToolbar.add(new JLabel("订单ID（可不填）"));
        queryToolbar.add(queryOrderIdField);
        queryToolbar.add(new JLabel("订单状态"));
        queryToolbar.add(queryStatusBox);
        queryToolbar.add(listButton);
        queryToolbar.add(refreshButton);

        JPanel controls = new JPanel(new GridLayout(isCurrentAdmin() ? 2 : 1, 1, 0, 8));
        controls.setOpaque(false);
        controls.add(wrapWithTitle("订单查询", queryToolbar));
        if (isCurrentAdmin()) {
            JPanel updateToolbar = toolbar();
            updateToolbar.add(new JLabel("订单ID（选择表格自动填）"));
            updateToolbar.add(updateOrderIdField);
            updateToolbar.add(new JLabel("改为"));
            updateToolbar.add(updateStatusBox);
            updateToolbar.add(updateButton);
            controls.add(wrapWithTitle("订单状态更新", updateToolbar));
        }

        table.getSelectionModel().addListSelectionListener(event -> {
            int row = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                updateOrderIdField.setText(String.valueOf(table.getValueAt(row, 0)));
            }
        });

        Runnable refreshOrders = () -> runTask("订单查询", () -> {
            Long queryUserId = isCurrentAdmin()
                    ? parseOptionalLong(userIdField.getText())
                    : Long.valueOf(requireCurrentUserId());
            return businessService.searchOrders(
                    queryUserId,
                    parseOptionalLong(queryOrderIdField.getText()),
                    selectedOrderStatus(queryStatusBox),
                    50,
                    0
            );
        }, orders -> {
            model.setRowCount(0);
            for (Order order : orders) {
                model.addRow(new Object[]{
                        order.getOrderId(), order.getUserId(), order.getItemId(), order.getQuantity(),
                        order.getUnitPrice(), discountText(order.getDiscountRate()), order.getAmount(),
                        order.getPaymentMethod(), formatOrderStatus(order.getStatus()), formatDate(order.getCreatedAt())
                });
            }
            setStatus("查询到 " + orders.size() + " 条订单");
        });
        listButton.addActionListener(event -> refreshOrders.run());
        refreshButton.addActionListener(event -> refreshOrders.run());

        updateButton.addActionListener(event -> runTask("更新订单状态", () -> businessService.updateOrderStatus(
                parseRequiredLong(updateOrderIdField.getText(), "订单ID"),
                updateStatusBox.getSelectedIndex()
        ), updated -> setStatus(updated ? "订单状态已更新" : "订单状态未变化")));

        panel.add(controls, BorderLayout.NORTH);
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
        JComboBox<CategoryChoice> itemCategoryBox = new JComboBox<>(categoryChoicesWithoutAll());
        JTextField itemDescField = new JTextField(20);
        JTextField itemPriceField = new JTextField("80.00", 8);
        JTextField itemDiscountField = new JTextField("0", 5);
        JTextField itemStatusIdField = new JTextField(8);
        JComboBox<String> itemStatusBox = new JComboBox<>(new String[]{"0-下架", "1-上架"});

        JPanel categoryToolbar = toolbar();
        JButton refreshCategoryButton = new JButton("刷新分类");
        JButton createCategoryButton = new JButton("新增分类");
        categoryToolbar.add(refreshCategoryButton);
        categoryToolbar.add(new JLabel("分类名"));
        categoryToolbar.add(categoryNameField);
        categoryToolbar.add(new JLabel("父ID"));
        categoryToolbar.add(parentIdField);
        categoryToolbar.add(createCategoryButton);

        JPanel itemToolbar = toolbar();
        JButton createItemButton = new JButton("新增景点");
        JButton itemStatusButton = new JButton("更新状态");
        JButton pricingButton = new JButton("修改价格折扣");
        itemToolbar.add(new JLabel("景点标题"));
        itemToolbar.add(itemTitleField);
        itemToolbar.add(new JLabel("预设类型"));
        itemToolbar.add(itemCategoryBox);
        itemToolbar.add(new JLabel("描述"));
        itemToolbar.add(itemDescField);
        itemToolbar.add(new JLabel("票价"));
        itemToolbar.add(itemPriceField);
        itemToolbar.add(new JLabel("折扣%"));
        itemToolbar.add(itemDiscountField);
        itemToolbar.add(createItemButton);
        itemToolbar.add(new JLabel("景点ID"));
        itemToolbar.add(itemStatusIdField);
        itemToolbar.add(itemStatusBox);
        itemToolbar.add(itemStatusButton);
        itemToolbar.add(pricingButton);

        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 8));
        controls.setOpaque(false);
        controls.add(wrapWithTitle("分类管理", categoryToolbar));
        controls.add(wrapWithTitle("景点管理", itemToolbar));

        refreshCategoryButton.addActionListener(event -> refreshCategories(categoryModel));
        createCategoryButton.addActionListener(event -> runAdminTask("新增分类", () -> businessService.createCategory(
                categoryNameField.getText(), parseOptionalLong(parentIdField.getText())
        ), id -> {
            resultArea.setText("分类创建成功，ID：" + id);
            refreshCategories(categoryModel);
        }));

        createItemButton.addActionListener(event -> runAdminTask("新增景点", () -> businessService.createItem(
                itemTitleField.getText(),
                requireCategoryId(itemCategoryBox),
                itemDescField.getText(),
                List.of(),
                new Document("source", "Swing后台"),
                parseRequiredAmount(itemPriceField.getText(), "票价"),
                parseRequiredAmount(itemDiscountField.getText(), "折扣")
        ), id -> resultArea.setText("景点创建成功，ID：" + id)));

        itemStatusButton.addActionListener(event -> runAdminTask("更新景点状态", () -> businessService.updateItemStatus(
                parseRequiredLong(itemStatusIdField.getText(), "景点ID"),
                itemStatusBox.getSelectedIndex()
        ), updated -> resultArea.setText(updated ? "景点状态已更新" : "景点状态未变化")));

        pricingButton.addActionListener(event -> runAdminTask("修改价格折扣", () -> businessService.updateItemPricing(
                parseRequiredLong(itemStatusIdField.getText(), "景点ID"),
                parseRequiredAmount(itemPriceField.getText(), "票价"),
                parseRequiredAmount(itemDiscountField.getText(), "折扣")
        ), updated -> resultArea.setText(updated ? "景点票价和折扣已更新" : "景点票价和折扣未变化")));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(categoryTable), wrapWithTitle("操作结果", resultArea));
        splitPane.setResizeWeight(0.68);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
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
        JButton userButton = new JButton(isCurrentAdmin() ? "用户报告" : "我的报告");
        JButton dashboardButton = new JButton("仪表盘汇总");
        JButton refreshButton = new JButton("刷新报表");
        toolbar.add(new JLabel("年份"));
        toolbar.add(yearField);
        toolbar.add(new JLabel("月份"));
        toolbar.add(monthField);
        toolbar.add(monthlyButton);
        toolbar.add(hotButton);
        if (isCurrentAdmin()) {
            toolbar.add(new JLabel("用户ID（可不填）"));
            toolbar.add(userIdField);
        }
        toolbar.add(userButton);
        toolbar.add(refreshButton);
        if (isCurrentAdmin()) {
            toolbar.add(dashboardButton);
        }

        monthlyButton.addActionListener(event -> runTask("月度订单报表", () -> statisticsService.getMonthlyOrderReport(
                parseRequiredInt(yearField.getText(), "年份"),
                parseRequiredInt(monthField.getText(), "月份")
        ), reports -> reportArea.setText(formatMonthlyReports(reports))));

        hotButton.addActionListener(event -> runTask("热门排行", () -> statisticsService.getHotItemRanking(null, null, 10),
                documents -> reportArea.setText(formatHotItems(documents))));

        Runnable refreshUserReport = () -> runTask("用户报告", () -> statisticsService.getUserReport(
                isCurrentAdmin() && !userIdField.getText().isBlank()
                        ? parseRequiredLong(userIdField.getText(), "用户ID")
                        : requireCurrentUserId(),
                null,
                null
        ), document -> reportArea.setText(formatUserReport(document)));
        userButton.addActionListener(event -> refreshUserReport.run());
        refreshButton.addActionListener(event -> refreshUserReport.run());

        dashboardButton.addActionListener(event -> runTask("仪表盘汇总", () -> statisticsService.buildDashboardReport(
                null, null, parseRequiredInt(yearField.getText(), "年份"), parseRequiredInt(monthField.getText(), "月份")
        ), dto -> reportArea.setText(new StringBuilder()
                .append("热门景点：").append(System.lineSeparator()).append(formatHotItems(dto.getHotItems()))
                .append(System.lineSeparator()).append("行为类型：").append(System.lineSeparator()).append(formatActionTypeSummary(dto.getActionTypeSummary()))
                .append(System.lineSeparator()).append("热门标签：").append(System.lineSeparator()).append(formatHotTags(dto.getHotTags()))
                .append(System.lineSeparator()).append("系统审计：").append(System.lineSeparator()).append(formatAuditSummary(dto.getSystemAuditSummary()))
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
        JButton refreshButton = new JButton("刷新日志");
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
        toolbar.add(refreshButton);

        Runnable refreshAuditLogs = () -> runAdminTask("审计日志查询", () -> systemLogService.queryAuditLogs(
                parseOptionalLong(userIdField.getText()),
                blankToNull(logTypeField.getText()),
                blankToNull((String) levelBox.getSelectedItem()),
                null,
                null,
                80
        ), documents -> setTextKeepingScroll(auditArea, formatAuditLogs(documents)));
        queryButton.addActionListener(event -> refreshAuditLogs.run());
        refreshButton.addActionListener(event -> refreshAuditLogs.run());

        summaryButton.addActionListener(event -> runAdminTask("审计汇总", () -> systemLogService.getAuditSummary(null, null),
                documents -> setTextKeepingScroll(auditArea, formatAuditSummary(documents))));

        trendButton.addActionListener(event -> runAdminTask("审计趋势", () -> systemLogService.getDailyAuditTrend(null, null),
                documents -> setTextKeepingScroll(auditArea, formatAuditTrend(documents))));

        userSummaryButton.addActionListener(event -> runAdminTask("用户操作汇总",
                () -> systemLogService.getUserOperationSummary(null, null, 50),
                documents -> setTextKeepingScroll(auditArea, formatUserOperationSummary(documents))));

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
        userLabel.setText("当前用户：" + currentUser.getUsername() + " / " + roleDisplay(currentUser.getRole())
                + " / ID " + currentUser.getUserId());
    }

    private void refreshHomeSummary() {
        if (currentUser == null) {
            homeUserValue.setText("未登录");
            homeRoleValue.setText("-");
        } else {
            homeUserValue.setText(currentUser.getUsername() + " / ID " + currentUser.getUserId());
            homeRoleValue.setText(roleDisplay(currentUser.getRole()));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("欢迎使用景点售票系统。").append(System.lineSeparator());
        builder.append("当前账号：")
                .append(currentUser == null ? "未登录" : currentUser.getUsername() + " / " + roleDisplay(currentUser.getRole()))
                .append(System.lineSeparator());
        builder.append("可用功能：个人档案、景点浏览、我的订单、统计报表。推荐入口已合并到景点浏览页。")
                .append(System.lineSeparator());
        if (currentUser != null && "ADMIN".equals(currentUser.getRole())) {
            builder.append("管理员功能：后台管理、系统审计。").append(System.lineSeparator());
        }
        homeSummaryArea.setText(builder.toString());
    }

    private void fillProfileForm(Profile profile, JTextField userIdField, JTextField realNameField,
                                 JTextField idCardField, JTextField addressField, JTextArea notesArea) {
        userIdField.setText(valueText(profile.getUserId()));
        realNameField.setText(fieldText(profile.getRealName()));
        idCardField.setText(fieldText(profile.getIdCard()));
        addressField.setText(fieldText(profile.getAddress()));
        notesArea.setText(fieldText(profile.getNotes()));
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    private void setColumnWidths(JTable table, int... widths) {
        int columnCount = Math.min(table.getColumnCount(), widths.length);
        for (int i = 0; i < columnCount; i += 1) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private long selectedOrTypedItemId(JTable table, JTextField itemIdField) {
        if (itemIdField.getText() != null && !itemIdField.getText().isBlank()) {
            return parseRequiredLong(itemIdField.getText(), "景点ID");
        }
        int row = table.getSelectedRow();
        if (row >= 0) {
            return parseRequiredLong(String.valueOf(table.getValueAt(row, 0)), "景点ID");
        }
        throw new IllegalArgumentException("请先在表格中选择一个景点，或手动填写景点ID");
    }

    private CategoryChoice[] categoryChoicesWithoutAll() {
        CategoryChoice[] choices = new CategoryChoice[CATEGORY_OPTIONS.length - 1];
        System.arraycopy(CATEGORY_OPTIONS, 1, choices, 0, choices.length);
        return choices;
    }

    private Long selectedCategoryId(JComboBox<CategoryChoice> categoryBox) {
        CategoryChoice choice = (CategoryChoice) categoryBox.getSelectedItem();
        return choice == null ? null : choice.categoryId();
    }

    private long requireCategoryId(JComboBox<CategoryChoice> categoryBox) {
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
        for (CategoryChoice choice : CATEGORY_OPTIONS) {
            if (categoryId.equals(choice.categoryId())) {
                return choice.label();
            }
        }
        return "类型ID " + categoryId;
    }

    private String discountText(BigDecimal discountRate) {
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) == 0) {
            return "无折扣";
        }
        return discountRate.stripTrailingZeros().toPlainString() + "%";
    }

    private String formatRecommendationScore(double score) {
        return String.format("%.1f", Math.max(0.0, Math.min(100.0, score)));
    }

    private Integer selectedOrderStatus(JComboBox<String> statusBox) {
        int selectedIndex = statusBox.getSelectedIndex();
        return selectedIndex <= 0 ? null : selectedIndex - 1;
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
        String processingStatus = name + "处理中...";
        setStatus(processingStatus);
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
        return switch (role == null ? "" : role) {
            case "ADMIN" -> "管理员";
            case "USER" -> "普通用户";
            default -> "未知";
        };
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
        if (value instanceof java.util.Date date) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME_FORMATTER);
        }
        return valueText(value);
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

    private record CategoryChoice(String label, Long categoryId) {
        @Override
        public String toString() {
            return label;
        }
    }
}
