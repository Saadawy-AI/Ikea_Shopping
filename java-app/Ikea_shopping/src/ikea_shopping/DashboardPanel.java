package ikea_shopping;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DashboardPanel extends JPanel {

    public DashboardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        Color ikeaBlue   = new Color(0, 88, 163);
        Color ikeaYellow = new Color(255, 189, 0);

        // ─── Title ───────────────────────────────────────────
        JLabel title = new JLabel("📊 Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(ikeaBlue);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // ─── Cards Panel ─────────────────────────────────────
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 15, 0));

        cardsPanel.add(createCard("📦 Products",   getCount("SELECT COUNT(*) FROM Products"),   ikeaBlue));
        cardsPanel.add(createCard("👤 Users",      getCount("SELECT COUNT(*) FROM Users"),      new Color(46, 139, 87)));
        cardsPanel.add(createCard("🛒 Orders",     getCount("SELECT COUNT(*) FROM Orders"),     new Color(180, 100, 0)));
        cardsPanel.add(createCard("💰 Total Sales","EGP " + getSum("SELECT SUM(total_price) FROM Orders"), new Color(150, 0, 0)));

        // ─── Bottom Panel ────────────────────────────────────
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        bottomPanel.add(createTopProductsTable());
        bottomPanel.add(createOrdersByStatusTable());

        // ─── Refresh Button ──────────────────────────────────
        JButton btnRefresh = new JButton("🔄 Refresh");
        btnRefresh.setBackground(ikeaYellow);
        btnRefresh.setFont(new Font("Arial", Font.BOLD, 13));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> refreshDashboard());

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.add(btnRefresh);

        // ─── Layout ──────────────────────────────────────────
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(title,        BorderLayout.NORTH);
        northPanel.add(cardsPanel,   BorderLayout.CENTER);
        northPanel.add(refreshPanel, BorderLayout.SOUTH);

        add(northPanel,   BorderLayout.NORTH);
        add(bottomPanel,  BorderLayout.CENTER);
    }

    // ─── Create Card ─────────────────────────────────────────
    private JPanel createCard(String label, String value, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(color);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        lblValue.setFont(new Font("Arial", Font.BOLD, 28));
        lblValue.setForeground(Color.WHITE);

        JLabel lblLabel = new JLabel(label, SwingConstants.CENTER);
        lblLabel.setFont(new Font("Arial", Font.BOLD, 14));
        lblLabel.setForeground(Color.WHITE);

        card.add(lblValue);
        card.add(lblLabel);

        return card;
    }

    // ─── Top Products Table ──────────────────────────────────
    private JPanel createTopProductsTable() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("🏆 Top 5 Products"));

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Product", "Total Sold"}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(25);

        String sql = "SELECT TOP 5 p.name, SUM(oi.quantity) AS total_sold "
                   + "FROM Order_Items oi "
                   + "JOIN Products p ON oi.product_id = p.product_id "
                   + "GROUP BY p.name "
                   + "ORDER BY total_sold DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getInt("total_sold")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ─── Orders By Status Table ──────────────────────────────
    private JPanel createOrdersByStatusTable() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("📋 Orders By Status"));

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Status", "Count"}, 0
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(25);

        String sql = "SELECT status, COUNT(*) AS count FROM Orders GROUP BY status";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("status"),
                    rs.getInt("count")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ─── Get Count ───────────────────────────────────────────
    private String getCount(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (SQLException e) {
            return "N/A";
        }
        return "0";
    }

    // ─── Get Sum ─────────────────────────────────────────────
    private String getSum(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return String.format("%.2f", rs.getDouble(1));
        } catch (SQLException e) {
            return "N/A";
        }
        return "0.00";
    }

    // ─── Refresh ─────────────────────────────────────────────
    private void refreshDashboard() {
        Container parent = getParent();
        int index = -1;
        if (parent instanceof JTabbedPane) {
            JTabbedPane tp = (JTabbedPane) parent;
            index = tp.indexOfComponent(this);
            tp.setComponentAt(index, new DashboardPanel());
            tp.setSelectedIndex(index);
        }
    }
}