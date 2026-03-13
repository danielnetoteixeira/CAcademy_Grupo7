package cncs.academy.ess.model;

public class User {
    private int id;
    private String username;
    private String password;
    private String salt;
    public User(int id, String username, String password, String salt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.salt=salt;
    }
    public User(String username, String password, String salt) {
        this.username = username;
        this.password = password;
        this.salt=salt;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getSalt() {
        return salt;
    }
}

