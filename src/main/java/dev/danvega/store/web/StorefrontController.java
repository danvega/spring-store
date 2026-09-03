package dev.danvega.store.web;

import dev.danvega.store.catalog.StickerRepository;
import dev.danvega.store.checkout.CheckoutService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
class StorefrontController {

    private final StickerRepository stickers;
    private final CheckoutService checkout;

    StorefrontController(StickerRepository stickers, CheckoutService checkout) {
        this.stickers = stickers;
        this.checkout = checkout;
    }

    @GetMapping("/")
    String storefront(Model model) {
        model.addAttribute("stickers", stickers.findAllByOrderBySortOrderAsc());
        return "storefront";
    }

    @PostMapping("/buy/{slug}")
    String buy(@PathVariable String slug) {
        var sticker = stickers.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No sticker called " + slug));
        return "redirect:" + checkout.start(sticker);
    }
}
