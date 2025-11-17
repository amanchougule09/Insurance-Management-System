import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.toedter.calendar.JDateChooser;
import java.util.Date;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.ResultSet;

public class InsuranceSystem extends JFrame implements LoginPanel.LoginListener {
    // ==========================================
    // Class Variables and Fields
    // ==========================================
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LoginPanel loginPanel;
    private JPanel contentPanel;
    private JPanel customerDetailsPanel;
    private JPanel footerPanel;
    private Image backgroundImage;
    private Connection connection;
    private JLabel dbStatusLabel;

    // ==========================================
    // Database Connection
    // ==========================================
    private void initializeDatabase() {
        try {
            File driverFile = new File("lib/ojdbc8.jar");
            if (!driverFile.exists()) {
                throw new ClassNotFoundException("ojdbc8.jar not found in lib directory");
            }

            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{driverFile.toURI().toURL()},
                InsuranceSystem.class.getClassLoader()
            );
            
            Class.forName("oracle.jdbc.OracleDriver", true, classLoader);

            String url = "jdbc:oracle:thin:@localhost:1521:XE";
            String username = "system";
            String password = "system";

            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Database connected successfully!");
            updateDatabaseStatus(true, "Connected");
            
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC Driver not found: " + e.getMessage());
            updateDatabaseStatus(false, "Driver not found");
            JOptionPane.showMessageDialog(this,
                "Database driver not found. Please ensure ojdbc8.jar is in the lib directory.",
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            updateDatabaseStatus(false, "Connection failed");
            JOptionPane.showMessageDialog(this,
                "Could not connect to database: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            updateDatabaseStatus(false, "Error");
            JOptionPane.showMessageDialog(this,
                "Error initializing database: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDatabaseStatus(boolean connected, String message) {
        if (dbStatusLabel != null) {
            dbStatusLabel.setText("Database: " + message);
            dbStatusLabel.setForeground(connected ? new Color(46, 204, 113) : new Color(231, 76, 60));
        }
    }

    private void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    // ==========================================
    // Database Operations
    // ==========================================
    private int getNextCustomerId() {
        try {
            String sql = "SELECT NVL(MAX(id), 0) + 1 FROM customer_details";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 1; // Default to 1 if no records exist
            
        } catch (SQLException e) {
            System.err.println("Error getting next customer ID: " + e.getMessage());
            return 1; // Default to 1 if there's an error
        }
    }

    private void saveCustomerDetails(String name, String email, String phone, String address,
                                   String policyType, String policyNumber, Date startDate, Date endDate) {
        if (connection == null) {
            JOptionPane.showMessageDialog(this,
                "Database connection is not available. Please restart the application.",
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Get the next available ID
            int nextId = getNextCustomerId();
            
            String sql = "INSERT INTO customer_details (id, name, email, phone, address, policy_type, " +
                        "policy_number, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, nextId);
            pstmt.setString(2, name);
            pstmt.setString(3, email);
            pstmt.setString(4, phone);
            pstmt.setString(5, address);
            pstmt.setString(6, policyType);
            pstmt.setString(7, policyNumber);
            pstmt.setDate(8, new java.sql.Date(startDate.getTime()));
            pstmt.setDate(9, new java.sql.Date(endDate.getTime()));
            
            pstmt.executeUpdate();
            pstmt.close();
            
            updateDatabaseStatus(true, "Connected");
            
            // Show success message with the generated ID
            JOptionPane.showMessageDialog(this,
                "Customer details saved successfully!\nGenerated Customer ID: " + nextId,
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException e) {
            System.err.println("Error saving customer details: " + e.getMessage());
            updateDatabaseStatus(false, "Error saving data");
            JOptionPane.showMessageDialog(this,
                "Error saving data to database: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // Constructor and Initialization
    // ==========================================
    public InsuranceSystem() {
        loadBackgroundImage();
        initializeDatabase();
        initializeUI();
    }

    // ==========================================
    // Background Image Handling
    // ==========================================
    private void loadBackgroundImage() {
        try {
            backgroundImage = ImageIO.read(new File("main.jpg"));
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
        }
    }

    // ==========================================
    // Main UI Initialization
    // ==========================================
    private void initializeUI() {
        setTitle("Insurance Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Create card layout for switching between login and main content
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        // Create login panel
        loginPanel = new LoginPanel(this);

        // Create main content panel with header, content, and footer
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185, 230));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 60));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // System name label (centered)
        JLabel systemNameLabel = new JLabel("Insurance Management System");
        systemNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        systemNameLabel.setForeground(Color.WHITE);
        systemNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(systemNameLabel, BorderLayout.CENTER);

        // Database status label
        dbStatusLabel = new JLabel("Database: Connecting...");
        dbStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dbStatusLabel.setForeground(Color.WHITE);
        headerPanel.add(dbStatusLabel, BorderLayout.WEST);

        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(new Color(231, 76, 60));
        logoutButton.setFocusPainted(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> handleLogout());
        headerPanel.add(logoutButton, BorderLayout.EAST);

        // Create customer details panel
        customerDetailsPanel = createCustomerDetailsPanel();

        // Create footer panel
        footerPanel = createFooterPanel();

        // Create a scrollable panel for the customer details
        JScrollPane scrollPane = new JScrollPane(customerDetailsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Customize scrollbar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        verticalScrollBar.setBackground(new Color(41, 128, 185, 230));
        verticalScrollBar.setForeground(Color.WHITE);
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(50);

        // Add all panels to content panel
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(footerPanel, BorderLayout.SOUTH);

        // Add panels to main panel
        mainPanel.add(loginPanel, "login");
        mainPanel.add(contentPanel, "content");

        // Show login panel first
        cardLayout.show(mainPanel, "login");

        add(mainPanel);
    }

    // ==========================================
    // Customer Details Panel Creation
    // ==========================================
    private JPanel createCustomerDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("Customer Details");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(41, 41, 41)); // Dark gray color
        titlePanel.add(titleLabel);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(titlePanel, gbc);

        // Personal Information Section
        JPanel personalInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        personalInfoPanel.setOpaque(false);
        JLabel personalLabel = new JLabel("Personal Information");
        personalLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        personalLabel.setForeground(new Color(41, 41, 41)); // Dark gray color
        personalInfoPanel.add(personalLabel);
        
        gbc.gridy = 1;
        panel.add(personalInfoPanel, gbc);

        // Personal Information Fields
        String[] personalLabels = {"Full Name:", "Email:", "Phone:", "Address:"};
        JTextField[] personalFields = new JTextField[personalLabels.length];
        
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        
        for (int i = 0; i < personalLabels.length; i++) {
            final int fieldIndex = i;
            gbc.gridy = fieldIndex + 2;
            
            // Label
            gbc.gridx = 0;
            JLabel label = new JLabel(personalLabels[fieldIndex]);
            label.setFont(new Font("Segoe UI", Font.BOLD, 16));
            label.setForeground(new Color(41, 41, 41)); // Dark gray color
            panel.add(label, gbc);
            
            // Text Field
            gbc.gridx = 1;
            personalFields[fieldIndex] = new JTextField(25);
            personalFields[fieldIndex].setFont(new Font("Segoe UI", Font.PLAIN, 16));
            personalFields[fieldIndex].setBackground(Color.WHITE);
            personalFields[fieldIndex].setForeground(new Color(41, 41, 41));
            personalFields[fieldIndex].setCaretColor(new Color(41, 128, 185));
            personalFields[fieldIndex].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            
            // Add hover effect
            personalFields[fieldIndex].addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    personalFields[fieldIndex].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(41, 128, 185), 2),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                    ));
                }
                public void mouseExited(MouseEvent e) {
                    if (!personalFields[fieldIndex].hasFocus()) {
                        personalFields[fieldIndex].setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                    }
                }
            });

            // Add focus effect
            personalFields[fieldIndex].addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    personalFields[fieldIndex].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(41, 128, 185), 2),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                    ));
                }
                public void focusLost(FocusEvent e) {
                    personalFields[fieldIndex].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                    ));
                }
            });
            
            // Add input validation
            personalFields[fieldIndex].getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) { validateField(fieldIndex); }
                public void removeUpdate(DocumentEvent e) { validateField(fieldIndex); }
                public void insertUpdate(DocumentEvent e) { validateField(fieldIndex); }
                
                private void validateField(int index) {
                    String text = personalFields[index].getText();
                    switch(index) {
                        case 0: // Name
                            if (!text.matches("^[a-zA-Z\\s]{2,50}$")) {
                                personalFields[index].setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                                ));
                            } else {
                                personalFields[index].setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                                ));
                            }
                            break;
                        case 1: // Email
                            if (!text.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                                personalFields[index].setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                                ));
                            } else {
                                personalFields[index].setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                                ));
                            }
                            break;
                        case 2: // Phone
                            if (!text.matches("^\\d{10}$")) {
                                personalFields[index].setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                                ));
                            } else {
                                personalFields[index].setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                                ));
                            }
                            break;
                    }
                }
            });
            
            panel.add(personalFields[fieldIndex], gbc);
        }

        // Policy Information Section
        JPanel policyInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        policyInfoPanel.setOpaque(false);
        JLabel policyLabel = new JLabel("Policy Information");
        policyLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        policyLabel.setForeground(new Color(41, 41, 41)); // Dark gray color
        policyInfoPanel.add(policyLabel);
        
        gbc.gridy = personalLabels.length + 2;
        panel.add(policyInfoPanel, gbc);

        // Policy Information Fields
        String[] policyLabels = {"Policy Type:", "Policy Number:", "Start Date:", "End Date:"};
        JComponent[] policyFields = new JComponent[policyLabels.length];
        
        for (int i = 0; i < policyLabels.length; i++) {
            final int fieldIndex = i;
            gbc.gridy = personalLabels.length + i + 3;
            
            // Label
            gbc.gridx = 0;
            JLabel label = new JLabel(policyLabels[i]);
            label.setFont(new Font("Segoe UI", Font.BOLD, 16));
            label.setForeground(new Color(41, 41, 41)); // Dark gray color
            
            // Add icon for date fields
            if (fieldIndex == 2 || fieldIndex == 3) {
                label.setIcon(new ImageIcon("calendar.png"));
                label.setIconTextGap(10);
                label.setHorizontalTextPosition(SwingConstants.LEFT);
                label.setVerticalTextPosition(SwingConstants.CENTER);
            }
            
            panel.add(label, gbc);
            
            // Field
            gbc.gridx = 1;
            if (fieldIndex == 0) { // Policy Type
                JComboBox<String> policyType = new JComboBox<>(new String[]{
                    "Health Insurance",
                    "Life Insurance",
                    "Auto Insurance",
                    "Home Insurance",
                    "Business Insurance"
                });
                policyType.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                policyType.setBackground(Color.WHITE);
                policyType.setForeground(new Color(41, 41, 41));
                policyType.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));

                // Add hover effect
                policyType.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        policyType.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(41, 128, 185), 2),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                    }
                    public void mouseExited(MouseEvent e) {
                        if (!policyType.hasFocus()) {
                            policyType.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                                BorderFactory.createEmptyBorder(8, 12, 8, 12)
                            ));
                        }
                    }
                });

                // Add focus effect
                policyType.addFocusListener(new FocusAdapter() {
                    public void focusGained(FocusEvent e) {
                        policyType.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(41, 128, 185), 2),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                    }
                    public void focusLost(FocusEvent e) {
                        policyType.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                    }
                });

                policyFields[fieldIndex] = policyType;
            } else if (fieldIndex == 1) { // Policy Number
                JTextField policyNumber = new JTextField(25);
                policyNumber.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                policyNumber.setBackground(Color.WHITE);
                policyNumber.setForeground(new Color(41, 41, 41));
                policyNumber.setCaretColor(new Color(41, 128, 185));
                policyNumber.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
                policyFields[fieldIndex] = policyNumber;
            } else { // Dates
                JDateChooser dateChooser = new JDateChooser();
                dateChooser.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                dateChooser.setBackground(Color.WHITE);
                dateChooser.setForeground(new Color(41, 41, 41));
                dateChooser.setDate(new Date());
                dateChooser.setDateFormatString("dd/MM/yyyy");
                
                // Customize the date chooser appearance
                dateChooser.getJCalendar().setBackground(Color.WHITE);
                dateChooser.getJCalendar().setForeground(new Color(41, 41, 41));
                dateChooser.getJCalendar().setFont(new Font("Segoe UI", Font.PLAIN, 14));
                
                // Set the button appearance
                JButton dateButton = dateChooser.getCalendarButton();
                dateButton.setBackground(Color.WHITE);
                dateButton.setForeground(new Color(41, 41, 41));
                dateButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
                dateButton.setBorderPainted(false);
                dateButton.setFocusPainted(false);
                
                // Create a panel to hold the date chooser with proper styling
                JPanel datePanel = new JPanel(new BorderLayout());
                datePanel.setOpaque(false);
                datePanel.add(dateChooser, BorderLayout.CENTER);
                datePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));

                // Add hover effect for date panel
                datePanel.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        datePanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(41, 128, 185), 2),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                        dateButton.setBackground(new Color(41, 128, 185));
                        dateButton.setForeground(Color.WHITE);
                    }
                    public void mouseExited(MouseEvent e) {
                        datePanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                        dateButton.setBackground(Color.WHITE);
                        dateButton.setForeground(new Color(41, 41, 41));
                    }
                });

                // Add focus effect for date panel
                datePanel.addFocusListener(new FocusAdapter() {
                    public void focusGained(FocusEvent e) {
                        datePanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(41, 128, 185), 2),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                        dateButton.setBackground(new Color(41, 128, 185));
                        dateButton.setForeground(Color.WHITE);
                    }
                    public void focusLost(FocusEvent e) {
                        datePanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(41, 41, 41), 2),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                        dateButton.setBackground(Color.WHITE);
                        dateButton.setForeground(new Color(41, 41, 41));
                    }
                });
                
                policyFields[fieldIndex] = datePanel;
            }
            
            panel.add(policyFields[fieldIndex], gbc);
        }

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15)); // Increased spacing
        buttonPanel.setOpaque(false);

        // Submit Button
        JButton submitButton = createStyledButton("Submit", new Color(41, 128, 185));
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 18)); // Enhanced button font
        submitButton.addActionListener(e -> handleSubmit(personalFields, policyFields));
        
        // Clear Button
        JButton clearButton = createStyledButton("Clear", new Color(231, 76, 60));
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 18)); // Enhanced button font
        clearButton.addActionListener(e -> clearFields(personalFields, policyFields));
        
        buttonPanel.add(submitButton);
        buttonPanel.add(clearButton);

        gbc.gridy = personalLabels.length + policyLabels.length + 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        return panel;
    }

    // ==========================================
    // Section Panel Creation
    // ==========================================
    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setForeground(new Color(41, 128, 185));
        panel.add(label);
        return panel;
    }

    // ==========================================
    // Button Styling and Creation
    // ==========================================
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 45)); // Larger buttons
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }

    // ==========================================
    // Field Validation
    // ==========================================
    private boolean validateFields(JTextField[] personalFields, JComponent[] policyFields) {
        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();

        // Validate Personal Information
        // Name validation
        String name = personalFields[0].getText().trim();
        if (name.isEmpty() || !name.matches("^[a-zA-Z\\s]{2,50}$")) {
            errorMessage.append("• Name must be 2-50 characters long and contain only letters and spaces\n");
            personalFields[0].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            isValid = false;
        }

        // Email validation
        String email = personalFields[1].getText().trim();
        if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errorMessage.append("• Please enter a valid email address\n");
            personalFields[1].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            isValid = false;
        }

        // Phone validation
        String phone = personalFields[2].getText().trim();
        if (phone.isEmpty() || !phone.matches("^\\d{10}$")) {
            errorMessage.append("• Phone number must be 10 digits\n");
            personalFields[2].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            isValid = false;
        }

        // Address validation
        String address = personalFields[3].getText().trim();
        if (address.isEmpty() || address.length() < 5) {
            errorMessage.append("• Address must be at least 5 characters long\n");
            personalFields[3].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            isValid = false;
        }

        // Validate Policy Information
        // Policy Type validation
        String policyType = ((JComboBox<?>)policyFields[0]).getSelectedItem().toString();
        if (policyType.isEmpty()) {
            errorMessage.append("• Please select a policy type\n");
            isValid = false;
        }

        // Policy Number validation
        String policyNumber = ((JTextField)policyFields[1]).getText().trim();
        if (policyNumber.isEmpty() || !policyNumber.matches("^[A-Z]{2}\\d{6}$")) {
            errorMessage.append("• Policy number must be in format: XX123456 (2 letters followed by 6 digits)\n");
            ((JTextField)policyFields[1]).setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            isValid = false;
        }

        // Date validation
        Date startDate = ((JDateChooser)((JPanel)policyFields[2]).getComponent(0)).getDate();
        Date endDate = ((JDateChooser)((JPanel)policyFields[3]).getComponent(0)).getDate();
        Date currentDate = new Date();

        if (startDate == null) {
            errorMessage.append("• Please select a start date\n");
            isValid = false;
        } else if (startDate.before(currentDate)) {
            errorMessage.append("• Start date cannot be in the past\n");
            isValid = false;
        }

        if (endDate == null) {
            errorMessage.append("• Please select an end date\n");
            isValid = false;
        } else if (endDate.before(startDate)) {
            errorMessage.append("• End date must be after start date\n");
            isValid = false;
        }

        if (!isValid) {
            // Create custom error dialog
            JDialog errorDialog = new JDialog(this, "Validation Error", true);
            errorDialog.setLayout(new BorderLayout());
            
            // Create header panel
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            headerPanel.setBackground(new Color(231, 76, 60));
            JLabel headerLabel = new JLabel("Please correct the following errors:");
            headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            headerLabel.setForeground(Color.WHITE);
            headerPanel.add(headerLabel);
            
            // Create message panel
            JPanel messagePanel = new JPanel(new BorderLayout());
            messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            JTextArea messageArea = new JTextArea(errorMessage.toString());
            messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageArea.setForeground(new Color(41, 41, 41));
            messageArea.setBackground(Color.WHITE);
            messageArea.setEditable(false);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messagePanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
            
            // Create button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton okButton = new JButton("OK");
            okButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            okButton.setForeground(Color.WHITE);
            okButton.setBackground(new Color(231, 76, 60));
            okButton.setFocusPainted(false);
            okButton.setBorderPainted(false);
            okButton.addActionListener(e -> errorDialog.dispose());
            buttonPanel.add(okButton);
            
            errorDialog.add(headerPanel, BorderLayout.NORTH);
            errorDialog.add(messagePanel, BorderLayout.CENTER);
            errorDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            errorDialog.setSize(400, 300);
            errorDialog.setLocationRelativeTo(this);
            errorDialog.setVisible(true);
        }

        return isValid;
    }

    // ==========================================
    // Form Submission Handling
    // ==========================================
    private void handleSubmit(JTextField[] personalFields, JComponent[] policyFields) {
        if (validateFields(personalFields, policyFields)) {
            // Save to database
            saveCustomerDetails(
                personalFields[0].getText(),
                personalFields[1].getText(),
                personalFields[2].getText(),
                personalFields[3].getText(),
                ((JComboBox<?>)policyFields[0]).getSelectedItem().toString(),
                ((JTextField)policyFields[1]).getText(),
                ((JDateChooser)((JPanel)policyFields[2]).getComponent(0)).getDate(),
                ((JDateChooser)((JPanel)policyFields[3]).getComponent(0)).getDate()
            );

            // Create success dialog
            JDialog successDialog = new JDialog(this, "Success", true);
            successDialog.setLayout(new BorderLayout());
            
            // Create header panel
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            headerPanel.setBackground(new Color(46, 204, 113));
            JLabel headerLabel = new JLabel("Customer Details Submitted Successfully!");
            headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            headerLabel.setForeground(Color.WHITE);
            headerPanel.add(headerLabel);
            
            // Create message panel
            JPanel messagePanel = new JPanel(new BorderLayout());
            messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            StringBuilder message = new StringBuilder();
            message.append("Personal Information:\n");
            message.append("• Name: ").append(personalFields[0].getText()).append("\n");
            message.append("• Email: ").append(personalFields[1].getText()).append("\n");
            message.append("• Phone: ").append(personalFields[2].getText()).append("\n");
            message.append("• Address: ").append(personalFields[3].getText()).append("\n\n");
            
            message.append("Policy Information:\n");
            message.append("• Policy Type: ").append(((JComboBox<?>)policyFields[0]).getSelectedItem()).append("\n");
            message.append("• Policy Number: ").append(((JTextField)policyFields[1]).getText()).append("\n");
            message.append("• Start Date: ").append(((JDateChooser)((JPanel)policyFields[2]).getComponent(0)).getDate()).append("\n");
            message.append("• End Date: ").append(((JDateChooser)((JPanel)policyFields[3]).getComponent(0)).getDate());
            
            JTextArea messageArea = new JTextArea(message.toString());
            messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageArea.setForeground(new Color(41, 41, 41));
            messageArea.setBackground(Color.WHITE);
            messageArea.setEditable(false);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messagePanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
            
            // Create button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton okButton = new JButton("OK");
            okButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            okButton.setForeground(Color.WHITE);
            okButton.setBackground(new Color(46, 204, 113));
            okButton.setFocusPainted(false);
            okButton.setBorderPainted(false);
            okButton.addActionListener(e -> {
                successDialog.dispose();
                clearFields(personalFields, policyFields);
            });
            buttonPanel.add(okButton);
            
            successDialog.add(headerPanel, BorderLayout.NORTH);
            successDialog.add(messagePanel, BorderLayout.CENTER);
            successDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            successDialog.setSize(400, 400);
            successDialog.setLocationRelativeTo(this);
            successDialog.setVisible(true);
        }
    }

    // ==========================================
    // Field Clearing
    // ==========================================
    private void clearFields(JTextField[] personalFields, JComponent[] policyFields) {
        // Clear personal fields
        for (JTextField field : personalFields) {
            field.setText("");
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 200), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
        }
        
        // Clear policy fields
        ((JComboBox<?>)policyFields[0]).setSelectedIndex(0);
        ((JTextField)policyFields[1]).setText("");
        ((JTextField)policyFields[1]).setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 200), 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Set dates to current date
        Date currentDate = new Date();
        ((JDateChooser)((JPanel)policyFields[2]).getComponent(0)).setDate(currentDate);
        ((JDateChooser)((JPanel)policyFields[3]).getComponent(0)).setDate(currentDate);
    }

    // ==========================================
    // Footer Panel Creation
    // ==========================================
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(41, 128, 185, 230)); // Semi-transparent blue
        footer.setPreferredSize(new Dimension(getWidth(), 60));
        footer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Company Information
        JLabel companyInfo = new JLabel("© 2024 Insurance Management System | Contact: support@insurance.com | Phone: 1800-208-8787");
        companyInfo.setFont(new Font("Arial", Font.PLAIN, 12));
        companyInfo.setForeground(Color.WHITE);
        companyInfo.setHorizontalAlignment(SwingConstants.CENTER);
        footer.add(companyInfo, BorderLayout.CENTER);

        return footer;
    }

    // ==========================================
    // Logout Handling
    // ==========================================
    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            cardLayout.show(mainPanel, "login");
        }
    }

    // ==========================================
    // Login Success Handling
    // ==========================================
    @Override
    public void onLoginSuccess() {
        cardLayout.show(mainPanel, "content");
    }

    // ==========================================
    // Main Method
    // ==========================================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            InsuranceSystem system = new InsuranceSystem();
            system.setVisible(true);
            
            // Add window listener to close database connection
            system.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    system.closeDatabaseConnection();
                }
            });
        });
    }
}


