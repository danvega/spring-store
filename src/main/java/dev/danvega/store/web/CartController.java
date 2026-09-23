package dev.danvega.store.web;

import java.util.ArrayList;
import java.util.List;

import dev.danvega.store.cart.Cart;
import dev.danvega.store.cart.CartCookie;
import dev.danvega.store.cart.CartService;
import dev.danvega.store.catalog.Money;
import dev.danvega.store.catalog.StickerRepository;
import dev.danvega.store.checkout.CheckoutService;
import dev.danvega.store.checkout.QuantityTooLargeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
class CartController {

    private final CartService carts;
    private final CartCookie cookie;
    private final StickerRepository stickers;
    private final CheckoutService checkout;

    CartController(CartService carts, CartCookie cookie, StickerRepository stickers, CheckoutService checkout) {
        this.carts = carts;
        this.cookie = cookie;
        this.stickers = stickers;
        this.checkout = checkout;
    }

    @GetMapping("/cart")
    String cart(HttpServletRequest request, HttpServletResponse response, Model model) {
        return renderCart(carts.forCookie(cookie.resolveOrIssue(request, response)), model, null);
    }

    private String renderCart(Cart cart, Model model, String error) {
        if (cart.isEmpty()) {
            return "cart-empty";
        }
        var lines = view(cart);
        model.addAttribute("lines", lines);
        model.addAttribute("totalDisplay", Money.display(
                lines.stream().mapToLong(LineView::lineTotalCents).sum()));
        model.addAttribute("itemCount", cart.itemCount());
        model.addAttribute("error", error);
        return "cart";
    }

    @PostMapping("/cart/add/{slug}")
    String add(@PathVariable String slug, HttpServletRequest request, HttpServletResponse response) {
        var sticker = stickers.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No sticker called " + slug));
        carts.add(cookie.resolveOrIssue(request, response), sticker.id(), 1);
        return "redirect:/";
    }

    @PostMapping("/cart/quantity/{stickerId}")
    String quantity(@PathVariable Long stickerId, @RequestParam int quantity,
            HttpServletRequest request, HttpServletResponse response) {
        carts.setQuantity(cookie.resolveOrIssue(request, response), stickerId, Math.max(0, quantity));
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{stickerId}")
    String remove(@PathVariable Long stickerId, HttpServletRequest request, HttpServletResponse response) {
        carts.remove(cookie.resolveOrIssue(request, response), stickerId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    String checkout(HttpServletRequest request, HttpServletResponse response, Model model) {
        var cart = carts.forCookie(cookie.resolveOrIssue(request, response));
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        try {
            return "redirect:" + checkout.start(cart);
        }
        catch (QuantityTooLargeException e) {
            // Their cart is fine and their fault is nothing. Show it back with the reason
            // rather than a 500 from Stripe's API.
            return renderCart(cart, model, e.getMessage());
        }
    }

    private List<LineView> view(Cart cart) {
        var views = new ArrayList<LineView>();
        for (var line : cart.lines()) {
            stickers.findById(line.stickerId())
                    .ifPresent(sticker -> views.add(new LineView(sticker, line.quantity(), sticker.priceCents())));
        }
        // A Set comes back from the database in hash order, so without this the lines
        // render in an order unrelated to how they were added. Match the catalog instead.
        views.sort(java.util.Comparator.comparingInt(v -> v.sticker().sortOrder()));
        return views;
    }
}
