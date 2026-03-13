package cncs.academy.ess.controller;

import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.UserRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import org.casbin.jcasbin.main.Enforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationMiddleware implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationMiddleware.class);
    private final UserRepository userRepository;
    private final Enforcer enforcer;

    public AuthorizationMiddleware(UserRepository userRepository, Enforcer enforcer) {
        this.userRepository = userRepository;
        this.enforcer = enforcer;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        // if method is OPTIONS bypass auth middleware
        if (ctx.method() == HandlerType.OPTIONS) {
            // Optionally: validate if it is a legitimate CORS preflight
            return;
        }


        // Allow unauthenticated requests to /user (register) and /login
        if (ctx.path().equals("/user") && ctx.method().name().equals("POST") ||
                ctx.path().equals("/login") && ctx.method().name().equals("POST"))
            return;

        // Check if authorization header exists
        String authorizationHeader = ctx.header("Authorization");
        String path = ctx.path();
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logger.info("Authorization header is missing or invalid '{}' for path '{}'", authorizationHeader, path);
            throw new UnauthorizedResponse();
        }

        // Extract token from authorization header
        String token = authorizationHeader.substring(7); // Remove "Bearer "

        // Check if token is valid (perform authentication logic)
        int userId = validateTokenAndGetUserId(token);
        if (userId == -1) {
            logger.info("Authorization token is invalid {}", token  );
            throw new UnauthorizedResponse();
        }

        if(!checkAccessControl(ctx, userId)){
            throw new UnauthorizedResponse();
        }

        // Add user ID to context for use in route handlers
        ctx.attribute("userId", userId);
    }

    private boolean checkAccessControl(Context ctx, int userId) {
        String path = ctx.path();
        String method = ctx.method().name();
        String userName = userRepository.findById(userId).getUsername();
        logger.info("Check path {} and method {} for user ID {}", path, method, userName);
        return enforcer.enforce(userName, path, method);
    }


    /**
     * NOTE: This method currently uses username lookup as a placeholder for real token validation.
     * Replace with proper token parsing/verification (e.g., JWT, session lookup) as needed.
     */
    /*private Integer validateTokenAndGetUserId(String token) {
        // Placeholder behavior: treat token as username (legacy behavior)
        User user = userRepository.findByUsername(token);
        if (user == null) {
            return -1;
        }
        return user.getId();
    }*/

    private Integer validateTokenAndGetUserId(String token) {
        try {
            // Verify signature + registered claims (exp, nbf, iat handled by the verifier)

            String issuer   = "auth0";
            String secret   = "o_meu_segredo_magnifico"; // DO NOT hardcode; load from secret store

            Algorithm alg = Algorithm.HMAC256(secret);
            JWTVerifier jwtVerifier = JWT.require(alg)
                    .withIssuer(issuer)
                    .acceptLeeway(5) // seconds of clock skew
                    .build();

            DecodedJWT jwt = jwtVerifier.verify(token);


            // 2) Fallback: subject ("sub") equals username
            String subject = jwt.getSubject();
            if (subject != null && !subject.isBlank()) {
                User user = userRepository.findByUsername(subject);
                if (user != null) {
                    return user.getId();
                } else {
                    logger.info("No user found for subject '{}'", subject);
                    return -1;
                }
            }

            // 3) No usable identity claim
            logger.info("JWT contains no 'uid' or 'sub' to resolve user identity");
            return -1;

        } catch (JWTVerificationException ex) {
            // Signature invalid, expired, wrong issuer/audience, etc.
            logger.info("JWT verification failed: {}", ex.getMessage());
            return -1;
        } catch (Exception ex) {
            logger.error("Unexpected error verifying JWT", ex);
            return -1;
        }
    }

}

