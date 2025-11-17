import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class LoginPanel extends JPanel {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private LoginListener loginListener;
    private Image backgroundImage;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public interface LoginListener {
        void onLoginSuccess();
    }

    public LoginPanel(LoginListener listener) {
        this.loginListener = listener;
        loadBackgroundImage();
        initializeUI();
    }

    private void loadBackgroundImage() {
        try {
            backgroundImage = ImageIO.read(new File("login.jpg"));
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private void initializeUI() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create login panel
        JPanel loginFormPanel = new JPanel(null);
        loginFormPanel.setPreferredSize(new Dimension(400, 400));
        loginFormPanel.setBackground(new Color(255, 255, 255, 230));
        loginFormPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Title
        JLabel title = new JLabel("Insurance Login", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setBounds(50, 30, 300, 40);
        loginFormPanel.add(title);

        // Username
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userLabel.setBounds(50, 100, 100, 25);
        loginFormPanel.add(userLabel);

        usernameField = new JTextField();
        usernameField.setBounds(50, 130, 300, 35);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        loginFormPanel.add(usernameField);

        // Password
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        passLabel.setBounds(50, 180, 100, 25);
        loginFormPanel.add(passLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(50, 210, 300, 35);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        loginFormPanel.add(passwordField);

        // Login Button
        JButton loginBtn = new JButton("Login");
        loginBtn.setBounds(50, 270, 300, 40);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 16));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBackground(new Color(41, 128, 185));
        loginBtn.setFocusPainted(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        loginBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                loginBtn.setBackground(new Color(52, 152, 219));
            }
            public void mouseExited(MouseEvent e) {
                loginBtn.setBackground(new Color(41, 128, 185));
            }
        });
        
        loginBtn.addActionListener(e -> handleLogin());
        loginFormPanel.add(loginBtn);

        // Sign Up Button
        JButton signUpBtn = new JButton("Sign Up");
        signUpBtn.setBounds(50, 320, 300, 40);
        signUpBtn.setFont(new Font("Arial", Font.BOLD, 16));
        signUpBtn.setForeground(Color.WHITE);
        signUpBtn.setBackground(new Color(46, 204, 113));
        signUpBtn.setFocusPainted(false);
        signUpBtn.setBorderPainted(false);
        signUpBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        signUpBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                signUpBtn.setBackground(new Color(39, 174, 96));
            }
            public void mouseExited(MouseEvent e) {
                signUpBtn.setBackground(new Color(46, 204, 113));
            }
        });
        
        signUpBtn.addActionListener(e -> showSignUpPanel());
        loginFormPanel.add(signUpBtn);

        // Status Label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setBounds(50, 370, 300, 25);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loginFormPanel.add(statusLabel);

        // Add a decorative line
        JSeparator separator = new JSeparator();
        separator.setBounds(50, 90, 300, 2);
        separator.setForeground(new Color(200, 200, 200));
        loginFormPanel.add(separator);

        // Create sign up panel
        SignUpPanel signUpPanel = new SignUpPanel(new SignUpPanel.SignUpListener() {
            @Override
            public void onSignUpSuccess() {
                showLoginPanel();
                statusLabel.setText("Account created successfully! Please login.");
                statusLabel.setForeground(new Color(46, 204, 113));
            }

            @Override
            public void onBackToLogin() {
                showLoginPanel();
            }
        });

        // Add panels to card layout
        mainPanel.add(loginFormPanel, "LOGIN");
        mainPanel.add(signUpPanel, "SIGNUP");

        // Add main panel to this panel
        setLayout(new GridBagLayout());
        add(mainPanel);
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password!");
            statusLabel.setForeground(Color.RED);
            return;
        }

        User user = UserService.getInstance().loginUser(username, password);
        if (user != null) {
            clearLoginFields();
            statusLabel.setText("");
            loginListener.onLoginSuccess();
        } else {
            statusLabel.setText("Invalid username or password!");
            statusLabel.setForeground(Color.RED);
        }
    }

    private void showSignUpPanel() {
        cardLayout.show(mainPanel, "SIGNUP");
    }

    private void showLoginPanel() {
        cardLayout.show(mainPanel, "LOGIN");
    }

    private void clearLoginFields() {
        usernameField.setText("");
        passwordField.setText("");
    }
} 