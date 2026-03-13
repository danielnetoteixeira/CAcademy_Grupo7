package cncs.academy.ess.service;

import cncs.academy.ess.helpers.HashingTools;
import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.UserRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static cncs.academy.ess.helpers.HashingTools.bytesToHex;

public class TodoUserService {
    private final UserRepository repository;

    public TodoUserService(UserRepository userRepository) {
        this.repository = userRepository;
    }
    public User addUser(String username, String password) throws Exception {
        byte[] salt = HashingTools.generateSalt();
        byte[] hashedPassword = HashingTools.hashPassword(password, salt, 10000,253);
        String hashedPasswordString = HashingTools.bytesToHex(hashedPassword);
        User user = new User(username, hashedPasswordString, HashingTools.bytesToHex(salt));
        int id = repository.save(user);
        user.setId(id);
        return user;
    }
    public User getUser(int id) {
        return repository.findById(id);
    }

    public void deleteUser(int id) {
        repository.deleteById(id);
    }

    public String login(String username, String password) throws Exception {
        User user = repository.findByUsername(username);
        if (user == null) {
            return null;
        }
        String userSalt = user.getSalt();
        byte[] hashedPassword = HashingTools.hashPassword(password,HashingTools.hexToBytes(userSalt), 10000,253);
        String hashedPasswordString = bytesToHex(hashedPassword);

        if (user.getPassword().equals(hashedPasswordString)) {
            return createAuthToken(user);
        }
        return null;
    }

    private String createAuthToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256("o_meu_segredo_magnifico");

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600); // 1 hour

        String token = JWT.create()
                .withIssuer("auth0")
                .withIssuedAt(Date.from(now))      // iat
                .withSubject(user.getUsername())     // nbf
                .withExpiresAt(Date.from(exp))     // exp
                .sign(algorithm);

        return "Bearer " + token;
    }
}
