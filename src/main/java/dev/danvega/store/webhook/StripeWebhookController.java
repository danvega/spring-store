package dev.danvega.store.webhook;

import java.time.Instant;
import java.util.Optional;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import dev.danvega.store.checkout.StripeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private static final String COMPLETED = "checkout.session.completed";

    private final StripeProperties properties;
    private final OrderRecorder recorder;

    StripeWebhookController(StripeProperties properties, OrderRecorder recorder) {
        this.properties = properties;
        this.recorder = recorder;
    }

    @PostMapping("/stripe/webhook")
    ResponseEntity<String> receive(@RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
        }
        catch (SignatureVerificationException e) {
            log.warn("Rejected a webhook with a bad signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("signature verification failed");
        }
        catch (RuntimeException e) {
            log.warn("Rejected an unreadable webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("unreadable payload");
        }

        if (!COMPLETED.equals(event.getType())) {
            return ResponseEntity.ok("ignored " + event.getType());
        }

        var session = sessionFrom(event);
        if (session.isEmpty()) {
            log.warn("Event {} had no readable checkout session", event.getId());
            return ResponseEntity.ok("no session on event");
        }

        var sessionId = session.get().getId();
        var outcome = recorder.record(event.getId(), event.getType(), sessionId,
                session.get().getClientReferenceId(), paidAt(event));

        return switch (outcome) {
            case RecordOutcome.Recorded(var order) -> ResponseEntity.ok("recorded " + order.reference());
            case RecordOutcome.AlreadySeen(var eventId) -> {
                // Stripe redelivers on any timeout or non-2xx. Saying OK to a repeat is
                // correct, and nothing was written the second time.
                log.info("Event {} was already handled, ignoring the redelivery", eventId);
                yield ResponseEntity.ok("already handled " + eventId);
            }
            case RecordOutcome.NoMatchingOrder(var missing) -> {
                // Stripe knows about a session this app never created. Acknowledge it so
                // Stripe stops retrying, but say so in the log.
                log.warn("No order found for checkout session {}", missing);
                yield ResponseEntity.ok("no matching order");
            }
        };
    }

    /**
     * Stripe's own API version can differ from the SDK's, which makes the typed
     * deserialization fail. deserializeUnsafe is the documented escape hatch.
     */
    private Optional<Session> sessionFrom(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().orElse(null) instanceof Session session) {
            return Optional.of(session);
        }
        try {
            if (deserializer.deserializeUnsafe() instanceof Session session) {
                return Optional.of(session);
            }
        }
        catch (EventDataObjectDeserializationException e) {
            log.warn("Could not deserialize event {}: {}", event.getId(), e.getMessage());
        }
        return Optional.empty();
    }

    private Instant paidAt(Event event) {
        return event.getCreated() == null ? Instant.now() : Instant.ofEpochSecond(event.getCreated());
    }
}
