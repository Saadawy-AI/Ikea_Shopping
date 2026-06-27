package ikea_shopping;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MainFrame extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private JTabbedPane tabbedPane;
    private String role;

    public MainFrame(String role) {
        this.role = role;

        setTitle("IKEA Shopping System" + (role.equals("admin") ? " - Admin" : " - User"));
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Colors
        Color ikeaBlue   = new Color(0, 88, 163);
        Color ikeaYellow = new Color(255, 189, 0);

        // Buttons
        JButton btnProducts   = new JButton("Products");
        JButton btnUsers      = new JButton("Users");
        JButton btnOrders     = new JButton("Orders");
        JButton btnAddresses  = new JButton("Addresses");
        JButton btnCategories = new JButton("Categories");
        JButton btnCart       = new JButton("Cart Items");
        JButton btnOrderItems = new JButton("Order Items");
        JButton btnPayments   = new JButton("Payments");
        JButton btnReviews    = new JButton("Reviews");

        for (JButton btn : new JButton[]{btnProducts, btnUsers, btnOrders,
                btnAddresses, btnCategories, btnCart,
                btnOrderItems, btnPayments, btnReviews}) {
            btn.setBackground(ikeaYellow);
            btn.setFont(new Font("Arial", Font.BOLD, 13));
            btn.setFocusPainted(false);
        }

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        topPanel.setBackground(ikeaBlue);
        topPanel.add(btnProducts);
        topPanel.add(btnUsers);
        topPanel.add(btnOrders);
        topPanel.add(btnAddresses);
        topPanel.add(btnCategories);
        topPanel.add(btnCart);
        topPanel.add(btnOrderItems);
        topPanel.add(btnPayments);
        topPanel.add(btnReviews);

        // Title
        JLabel title = new JLabel("IKEA Shopping System", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setOpaque(true);
        title.setBackground(ikeaBlue);
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // Role Label
        JLabel lblRole = new JLabel(role.equals("admin") ? "👮 Admin" : "👤 User", SwingConstants.RIGHT);
        lblRole.setFont(new Font("Arial", Font.BOLD, 13));
        lblRole.setForeground(Color.WHITE);
        lblRole.setOpaque(true);
        lblRole.setBackground(ikeaBlue);
        lblRole.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ikeaBlue);
        titlePanel.add(title,   BorderLayout.CENTER);
        titlePanel.add(lblRole, BorderLayout.EAST);

        // Table
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(table);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("📊 Dashboard", new DashboardPanel());

        if (role.equals("admin")) {
            tabbedPane.addTab("📦 Products CRUD", new ProductsPanel());
            tabbedPane.addTab("👤 Users CRUD",    new UsersPanel());
            tabbedPane.addTab("🛒 Orders CRUD",   new OrdersPanel());
        }

        tabbedPane.addTab("📋 View Data", scrollPane);

        // Layout
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(topPanel,   BorderLayout.SOUTH);

        setLayout(new BorderLayout(5, 5));
        add(northPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        // Actions - View Data index
        int viewIndex = role.equals("admin") ? 4 : 1;
        
        btnProducts.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadProducts(); });
        btnUsers.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadUsers(); });
        btnOrders.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadOrders(); });
        btnAddresses.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadAddresses(); });
        btnCategories.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadCategories(); });
        btnCart.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadCartItems(); });
        btnOrderItems.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadOrderItems(); });
        btnPayments.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadPayments(); });
        btnReviews.addActionListener(e -> { tabbedPane.setSelectedIndex(viewIndex); loadReviews(); });
    }

    // ─── helper ───────────────────────────────────────────────
    private void runQuery(String[] columns, String sql) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        for (String col : columns) tableModel.addColumn(col);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Object[] row = new Object[columns.length];
                for (int i = 0; i < columns.length; i++)
                    row[i] = rs.getObject(i + 1);
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ─── Products ─────────────────────────────────────────────
    private void loadProducts() {
        runQuery(
            new String[]{"ID", "Name", "Category", "Price", "Stock", "Active"},
            "SELECT p.product_id, p.name, c.name AS category, p.price, p.stock_qty, p.is_active "
          + "FROM Products p JOIN Categories c ON p.category_id = c.category_id"
        );
    }

    // ─── Users ────────────────────────────────────────────────
    private void loadUsers() {
        runQuery(
            new String[]{"ID", "First Name", "Last Name", "Email", "Phone", "Active"},
            "SELECT user_id, first_name, last_name, email, phone, is_active FROM Users"
        );
    }

    // ─── Orders ───────────────────────────────────────────────
    private void loadOrders() {
        runQuery(
            new String[]{"Order ID", "Customer", "Status", "Total", "Date"},
            "SELECT o.order_id, u.first_name + ' ' + u.last_name AS customer, "
          + "o.status, o.total_price, o.created_at "
          + "FROM Orders o JOIN Users u ON o.user_id = u.user_id"
        );
    }

    // ─── Addresses ────────────────────────────────────────────
    private void loadAddresses() {
        runQuery(
            new String[]{"ID", "User", "Street", "City", "Country", "Postal Code", "Default"},
            "SELECT a.address_id, u.first_name + ' ' + u.last_name AS user_name, "
          + "a.street, a.city, a.country, a.postal_code, a.is_default "
          + "FROM Addresses a JOIN Users u ON a.user_id = u.user_id"
        );
    }

    // ─── Categories ───────────────────────────────────────────
    private void loadCategories() {
        runQuery(
            new String[]{"ID", "Name", "Parent Category"},
            "SELECT c.category_id, c.name, "
          + "ISNULL(p.name, 'None') AS parent "
          + "FROM Categories c LEFT JOIN Categories p ON c.parent_id = p.category_id"
        );
    }

    // ─── Cart Items ───────────────────────────────────────────
    private void loadCartItems() {
        runQuery(
            new String[]{"Cart Item ID", "User", "Product", "Quantity"},
            "SELECT ci.cart_item_id, u.first_name + ' ' + u.last_name AS user_name, "
          + "p.name AS product, ci.quantity "
          + "FROM Cart_Items ci "
          + "JOIN Cart c ON ci.cart_id = c.cart_id "
          + "JOIN Users u ON c.user_id = u.user_id "
          + "JOIN Products p ON ci.product_id = p.product_id"
        );
    }

    // ─── Order Items ──────────────────────────────────────────
    private void loadOrderItems() {
        runQuery(
            new String[]{"Order Item ID", "Order ID", "Product", "Quantity", "Unit Price"},
            "SELECT oi.order_item_id, oi.order_id, p.name AS product, "
          + "oi.quantity, oi.unit_price "
          + "FROM Order_Items oi JOIN Products p ON oi.product_id = p.product_id"
        );
    }

    // ─── Payments ─────────────────────────────────────────────
    private void loadPayments() {
        runQuery(
            new String[]{"Payment ID", "Order ID", "Method", "Status", "Amount", "Paid At"},
            "SELECT payment_id, order_id, method, status, amount, paid_at FROM Payments"
        );
    }

    // ─── Reviews ──────────────────────────────────────────────
    private void loadReviews() {
        runQuery(
            new String[]{"Review ID", "User", "Product", "Rating", "Comment", "Date"},
            "SELECT r.review_id, u.first_name + ' ' + u.last_name AS user_name, "
          + "p.name AS product, r.rating, r.comment, r.created_at "
          + "FROM Reviews r "
          + "JOIN Users u ON r.user_id = u.user_id "
          + "JOIN Products p ON r.product_id = p.product_id"
        );
    }
}