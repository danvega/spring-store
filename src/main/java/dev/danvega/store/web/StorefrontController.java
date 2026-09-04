package dev.danvega.store.web;

import dev.danvega.store.cart.CartCookie;
import dev.danvega.store.cart.CartService;
import dev.danvega.store.catalog.StickerRepository;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class StorefrontController {

    private final StickerRepository stickers;
    private final CartService carts;
    private final CartCookie cookie;

    StorefrontController(StickerRepository stickers, CartService carts, CartCookie cookie) {
        this.stickers = stickers;
        this.carts = carts;
        this.cookie = cookie;
    }

    /**
     * Reads the cookie without issuing one. Browsing should not hand out an identity to
     * somebody who has not put anything in a cart.
     */
    @GetMapping("/")
    String storefront(HttpServletRequest request, Model model) {
        var cart = cookie.read(request).map(carts::forCookie).orElse(null);
        model.addAttribute("stickers", stickers.findAllByOrderBySortOrderAsc());
        model.addAttribute("itemCount", cart == null ? 0 : cart.itemCount());
        model.addAttribute("inCart", cart == null
                ? java.util.Map.<Long, Integer>of()
                : cart.lines().stream().collect(java.util.stream.Collectors.toMap(
                        dev.danvega.store.cart.CartLine::stickerId,
                        dev.danvega.store.cart.CartLine::quantity)));
        return "storefront";
    }
}
