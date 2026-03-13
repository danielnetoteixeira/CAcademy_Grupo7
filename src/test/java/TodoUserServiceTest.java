import cncs.academy.ess.helpers.HashingTools;
import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.UserRepository;
import cncs.academy.ess.service.TodoUserService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoUserServiceTest {

    @Mock
    private UserRepository repository;

    @Test
    @DisplayName("login_shouldReturnValidJWTTokenWhenCredentialsMatch")
    void login_shouldReturnValidJWTTokenWhenCredentialsMatch() throws Exception {
        // Arrange
        TodoUserService service = new TodoUserService(repository);

        final String username = "daniel";
        final String password = "s3cret";
        final String saltHex  = "DEADBEEF";           // pode ser qualquer string
        final byte[] saltBytes = new byte[]{(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF};
        final byte[] hashedBytes = new byte[]{0x01, 0x02, 0x03};  // valor simbólico
        final String hashedHex = "010203";            // valor que o serviço espera ver em user.getPassword()


        try (MockedStatic<HashingTools> hashing = mockStatic(HashingTools.class)) {
            hashing.when(() -> HashingTools.hexToBytes(saltHex)).thenReturn(saltBytes);
            hashing.when(() -> HashingTools.hashPassword(password, saltBytes, 10000, 253))
                    .thenReturn(hashedBytes);
            hashing.when(() -> HashingTools.bytesToHex(hashedBytes)).thenReturn(hashedHex);

            User stored = new User(username, hashedHex, saltHex);
            stored.setId(1);
            when(repository.findByUsername(username)).thenReturn(stored);

            // Act
            String bearer = service.login(username, password);

            // Assert (1): prefixo "Bearer "
            assertNotNull(bearer, "O login deveria devolver um token");
            assertTrue(bearer.startsWith("Bearer "), "O token deve começar por 'Bearer '");

            // Remover o prefixo e validar o JWT
            String jwt = bearer.substring("Bearer ".length());

            // Assert (2): validar assinatura e claims
            Algorithm alg = Algorithm.HMAC256("o_meu_segredo_magnifico");
            DecodedJWT decoded = JWT.require(alg)
                    .withIssuer("auth0")
                    .build()
                    .verify(jwt); // falha se assinatura/issuer não baterem certo

            assertEquals(username, decoded.getSubject(), "O 'sub' deve ser o username");
            assertNotNull(decoded.getIssuedAt(), "'iat' deve estar presente");
            assertNotNull(decoded.getExpiresAt(), "'exp' deve estar presente");
        }
    }
}
