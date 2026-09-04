package dev.danvega.store.cart;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** There are no accounts, so this cookie is the only identity a visitor has. */
@Component
public class CartCookie {

    public static final String NAME = "cart_id";

    private static final Duration LIFETIME = Duration.ofDays(30);

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    /** Issues one only when the visitor has none, so a cart survives across requests. */
    public String resolveOrIssue(HttpServletRequest request, HttpServletResponse response) {
        return read(request).orElseGet(() -> {
            var issued = UUID.randomUUID().toString();
            response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, issued)
                    .path("/")
                    .httpOnly(true)
                    .sameSite("Lax")
                    .maxAge(LIFETIME)
                    .build()
                    .toString());
            return issued;
        });
    }
}
