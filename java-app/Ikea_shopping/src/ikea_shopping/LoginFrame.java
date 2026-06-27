package ikea_shopping;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {

    private JTextField txtEmail;
    private JPasswordField txtPassword;

    public LoginFrame() {
        setTitle("IKEA Shopping - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Colors
        Color ikeaBlue   = new Color(0, 88, 163);
        Color ikeaYellow = new Color(255, 189, 0);

        // Title
        JLabel title = new JLabel("IKEA Shopping System", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setOpaque(true);
        title.setBackground(ikeaBlue);
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // Fields
        JLabel lblEmail    = new JLabel("Email:");
        JLabel lblPassword = new JLabel("Password:");
        txtEmail    = new JTextField(20);
        txtPassword = new JPasswordField(20);

        lblEmail.setFont(new Font("Arial", Font.BOLD, 13));
        lblPassword.setFont(new Font("Arial", Font.BOLD, 13));

        // Button
        JButton btnLogin = new JButton("Login");
        btnLogin.setBackground(ikeaYellow);
        btnLogin.setFont(new Font("Arial", Font.BOLD, 14));
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(lblEmail, gbc);
        gbc.gridx = 1;
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(lblPassword, gbc);
        gbc.gridx = 1;
        formPanel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(btnLogin, gbc);

        // Layout
        setLayout(new BorderLayout());
        add(title,     BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);

        // Actions
        btnLogin.addActionListener(e -> login());
        txtPassword.addActionListener(e -> login());
    }

    private void login() {
        String email    = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter email and password.",
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "SELECT * FROM Users WHERE email = ? AND is_active = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (PasswordUtils.verify(password, storedHash)) {
                    String name = rs.getString("first_name") + " " + rs.getString("last_name");
                    String role = rs.getString("role");
                    JOptionPane.showMessageDialog(this,
                        "Welcome, " + name + "!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    new MainFrame(role).setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Invalid email or password.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Invalid email or password.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}