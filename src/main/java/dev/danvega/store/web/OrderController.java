package dev.danvega.store.web;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dev.danvega.store.catalog.StickerRepository;
import dev.danvega.store.order.ConfirmingStage;
import dev.danvega.store.order.PurchaseOrder;
import dev.danvega.store.order.PurchaseOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
class OrderController {

    private final PurchaseOrderRepository orders;
    private final StickerRepository stickers;

    OrderController(PurchaseOrderRepository orders, StickerRepository stickers) {
        this.orders = orders;
        this.stickers = stickers;
    }

    /**
     * Stripe sends the buyer here the moment they pay. The webhook that records the
     * payment is a separate delivery and may not have arrived yet, so this decides
     * between telling them it is done and telling them the truth.
     */
    @GetMapping("/order/confirm")
    String confirm(@RequestParam("session_id") String sessionId,
            @RequestParam(name = "since", required = false) String since, Model model) {

        var order = orders.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No order for that session"));

        model.addAttribute("order", order);
        model.addAttribute("lines", view(order));

        if (order.isRecorded()) {
            return "confirmation";
        }

        var now = Instant.now();
        var arrivedAt = arrivalFrom(since, now);
        model.addAttribute("stage", ConfirmingStage.of(arrivedAt, now));
        model.addAttribute("arrivedAt", arrivedAt);
        model.addAttribute("waitedSeconds", Math.max(0, Duration.between(arrivedAt, now).toSeconds()));
        model.addAttribute("refreshUrl", UriComponentsBuilder.fromPath("/order/confirm")
                .queryParam("session_id", sessionId)
                .queryParam("since", arrivedAt.getEpochSecond())
                .toUriString());
        return "confirming";
    }

    /** The cart still holds everything, so this offers the cart rather than one sticker. */
    @GetMapping("/cancelled")
    String cancelled() {
        return "cancelled";
    }

    private List<LineView> view(PurchaseOrder order) {
        var views = new ArrayList<LineView>();
        for (var line : order.lines()) {
            stickers.findById(line.stickerId())
                    .ifPresent(sticker -> views.add(new LineView(sticker, line.quantity(), line.unitPriceCents())));
        }
        // A Set comes back from the database in hash order, so without this the lines
        // render in an order unrelated to how they were added. Match the catalog instead.
        views.sort(java.util.Comparator.comparingInt(v -> v.sticker().sortOrder()));
        return views;
    }

    /**
     * The page carries this across its own refreshes, so it is ours, but it still arrives
     * as a query parameter a buyer can edit. Anything unusable means we do not know when
     * they got here, and "now" is the honest answer. A future value would otherwise pin
     * the page in its waiting state forever.
     */
    private static Instant arrivalFrom(String since, Instant now) {
        if (since == null || since.isBlank()) {
            return now;
        }
        try {
            var arrived = Instant.ofEpochSecond(Long.parseLong(since));
            return arrived.isAfter(now) ? now : arrived;
        }
        catch (NumberFormatException | DateTimeException e) {
            return now;
        }
    }
}
