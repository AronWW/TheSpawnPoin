package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.*;
import com.thespawnpoint.backend.entity.auth.EmailVerificationToken;
import com.thespawnpoint.backend.entity.auth.PasswordResetToken;
import com.thespawnpoint.backend.entity.user.Profile;
import com.thespawnpoint.backend.entity.user.PrivacySettings;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.EmailVerificationTokenRepository;
import com.thespawnpoint.backend.repository.PasswordResetTokenRepository;
import com.thespawnpoint.backend.repository.PrivacySettingsRepository;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import com.thespawnpoint.backend.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RNG = new SecureRandom();

    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final String REFRESH_HINT_COOKIE = "has_refresh_token";

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ProfileRepository profileRepository;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AchievementService achievementService;

    @Value("${app.email.verification-expiration}")
    private int verificationExpirationSeconds;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${app.cookies.secure:false}")
    private boolean cookiesSecure;

    @Value("${app.cookies.same-site:Lax}")
    private String cookiesSameSite;

    // РЕЄСТРАЦІЯ

    @Transactional
    public void register(RegisterDTO dto) {
        String email = normalizeEmail(dto.getEmail());

        Optional<User> existingOpt = userRepository.findByEmail(email);

        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();

            if (existing.isEmailVerified()) {
                throw new ApiException(HttpStatus.CONFLICT, "Email is already in use");
            }

            existing.setDisplayName(dto.getDisplayName());
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            userRepository.save(existing);

            profileRepository.findByUserId(existing.getId()).ifPresent(profile -> {
                profile.setFullName(dto.getDisplayName());
                profileRepository.save(profile);
            });

            sendVerificationCode(existing);
            return;
        }

        User user = User.builder()
                .displayName(dto.getDisplayName())
                .email(email)
                .password(passwordEncoder.encode(dto.getPassword()))
                .emailVerified(false)
                .build();

        userRepository.save(user);

        Profile profile = Profile.builder()
                .user(user)
                .fullName(dto.getDisplayName())
                .build();

        profileRepository.save(profile);

        PrivacySettings privacySettings = PrivacySettings.builder()
                .user(user)
                .build();
        privacySettingsRepository.save(privacySettings);

        sendVerificationCode(user);
    }

    @Transactional
    protected void sendVerificationCode(User user) {
        emailVerificationTokenRepository.findByUser(user)
                .ifPresent(emailVerificationTokenRepository::delete);

        emailVerificationTokenRepository.flush();

        String code = String.format("%06d", RNG.nextInt(1_000_000));

        EmailVerificationToken token = EmailVerificationToken.builder()
                .code(passwordEncoder.encode(code))
                .user(user)
                .expiresAt(Instant.now().plusSeconds(verificationExpirationSeconds))
                .lastSentAt(Instant.now())
                .build();

        emailVerificationTokenRepository.save(token);

        emailService.sendVerificationCode(user.getEmail(), code, user.getDisplayName());
    }

    // ПОВТОРНЕ НАДСИЛАННЯ КОДУ

    @Transactional
    public Map<String, Object> resendVerification(String email) {
        String normalized = normalizeEmail(email);

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isEmailVerified()) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already confirmed");
        }

        emailVerificationTokenRepository.findByUser(user).ifPresent(existing -> {
            long secondsSinceLastSend = Duration.between(existing.getLastSentAt(), Instant.now()).getSeconds();
            if (secondsSinceLastSend < RESEND_COOLDOWN_SECONDS) {
                long secondsLeft = RESEND_COOLDOWN_SECONDS - secondsSinceLastSend;
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + secondsLeft + " seconds before resending");
            }
        });

        sendVerificationCode(user);

        return Map.of(
                "message", "Code sent",
                "lastSentAt", Instant.now().toString()
        );
    }

    // ПІДТВЕРДЖЕННЯ EMAIL

    @Transactional
    public Map<String, Object> verifyEmail(VerifyEmailDTO dto, HttpServletResponse response) {
        String email = normalizeEmail(dto.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        EmailVerificationToken token = emailVerificationTokenRepository.findByUser(user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Code not found"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.GONE, "Code expired");
        }

        if (!passwordEncoder.matches(dto.getCode(), token.getCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect code");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        emailVerificationTokenRepository.delete(token);

        setAuthCookies(response, user.getEmail(), false);

        Long userId = user.getId();
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
            try {
                userRepository.findById(userId).ifPresent(u ->
                        achievementService.unlock(u, AchievementCatalog.WELCOME_ABOARD, "AUTO")
                );
            } catch (Exception ignored) {}
        });

        return buildAuthResponse(user, "Email successfully confirmed");
    }

    // ЛОГІН

    public Map<String, Object> login(LoginDTO dto, HttpServletResponse response) {
        String email = normalizeEmail(dto.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        }

        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Email not confirmed");
        }

        setAuthCookies(response, user.getEmail(), dto.isRememberMe());

        return buildAuthResponse(user, "Successful login");
    }

    // REFRESH ТОКЕНУ

    public Map<String, String> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refresh_token");

        if (refreshToken == null || !jwtUtil.isTokenValid(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }

        if (!"refresh".equals(jwtUtil.extractType(refreshToken))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect token type");
        }

        String email = normalizeEmail(jwtUtil.extractEmail(refreshToken));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Email not confirmed");
        }

        String newAccessToken = jwtUtil.generateAccessToken(email);
        setAccessTokenCookie(response, newAccessToken);

        return Map.of("message", "Token updated");
    }

    // ЛОГАУТ

    public Map<String, String> logout(HttpServletResponse response) {
        clearAuthCookies(response);
        return Map.of("message", "The exit is successful.");
    }

    // FORGOT PASSWORD

    public Map<String, String> forgotPassword(ForgotPasswordDTO dto) {
        String email = normalizeEmail(dto.getEmail());

        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.findByUser(user)
                    .ifPresent(passwordResetTokenRepository::delete);

            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(900))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetLink(user.getEmail(), token, user.getDisplayName());
        });

        return Map.of("message", "If the email exists, the letter has been sent.");
    }

    // RESET PASSWORD

    public Map<String, String> resetPassword(ResetPasswordDTO dto) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Token not found"));

        if (resetToken.isUsed()) {
            throw new ApiException(HttpStatus.CONFLICT, "Token already used");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.GONE, "Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return Map.of("message", "Password successfully changed");
    }

    // CHANGE PASSWORD

    public Map<String, String> changePassword(User user, ChangePasswordDTO dto) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password must be different from current");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        return Map.of("message", "Password successfully changed");
    }

    // HELPERS

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void setAuthCookies(HttpServletResponse response, String email, boolean rememberMe) {
        setAccessTokenCookie(response, jwtUtil.generateAccessToken(email));

        long refreshMs = rememberMe ? JwtUtil.REMEMBER_ME_MS : refreshTokenExpirationMs;
        setRefreshTokenCookie(response, jwtUtil.generateRefreshToken(email, rememberMe), refreshMs);
        setRefreshHintCookie(response, refreshMs);
    }

    private Map<String, Object> buildAuthResponse(User user, String message) {
        String avatarUrl = profileRepository.findByUserId(user.getId())
                .map(Profile::getAvatarUrl)
                .orElse(null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("id", user.getId());
        body.put("email", user.getEmail());
        body.put("displayName", user.getDisplayName());
        body.put("emailVerified", user.isEmailVerified());
        body.put("role", user.getRole().name());
        body.put("status", user.getStatus().name());
        body.put("lastSeen", user.getLastSeen() != null ? user.getLastSeen().toString() : null);
        body.put("avatarUrl", avatarUrl);
        body.put("banned", user.isBanned());
        body.put("banReason", user.getBanReason());
        return body;
    }

    private void setAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(cookiesSecure)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpirationMs))
                .sameSite(cookiesSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token, long refreshMs) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(cookiesSecure)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(refreshMs))
                .sameSite(cookiesSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(cookiesSecure)
                .path("/")
                .sameSite(cookiesSameSite)
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookiesSecure)
                .path("/api/auth/refresh")
                .sameSite(cookiesSameSite)
                .maxAge(0)
                .build();

        ResponseCookie refreshHintCookie = ResponseCookie.from(REFRESH_HINT_COOKIE, "")
                .httpOnly(false)
                .secure(cookiesSecure)
                .path("/")
                .sameSite(cookiesSameSite)
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshHintCookie.toString());
    }

    private void setRefreshHintCookie(HttpServletResponse response, long refreshMs) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_HINT_COOKIE, "1")
                .httpOnly(false)
                .secure(cookiesSecure)
                .path("/")
                .maxAge(Duration.ofMillis(refreshMs))
                .sameSite(cookiesSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
