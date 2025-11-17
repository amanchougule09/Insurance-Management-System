import java.util.ArrayList;
import java.util.List;

public class UserService {
    private static UserService instance;
    private List<User> users;

    private UserService() {
        users = new ArrayList<>();
        // Add default user
        users.add(new User("Aman", "aman@example.com", "Aman", "Aman123"));
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public boolean registerUser(User newUser) {
        // Check if username already exists
        for (User user : users) {
            if (user.getUsername().equals(newUser.getUsername())) {
                return false;
            }
        }
        users.add(newUser);
        return true;
    }

    public User loginUser(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public boolean isUsernameExists(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
} 