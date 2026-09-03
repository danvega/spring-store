package dev.danvega.store.checkout;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import dev.danvega.store.catalog.Sticker;
import org.springframework.stereotype.Component;

@Component
class StripeCheckoutGateway implements StripeGateway {

    private final StripeClient stripe;

    StripeCheckoutGateway(StripeClient stripe) {
        this.stripe = stripe;
    }

    @Override
    public CheckoutSession start(Sticker sticker, String orderReference, String successUrl, String cancelUrl) {
        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(orderReference)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount((long) sticker.priceCents())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(sticker.name())
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            var session = stripe.checkout().sessions().create(params);
            return new CheckoutSession(session.getId(), session.getUrl());
        }
        catch (StripeException e) {
            throw new CheckoutFailedException("Stripe would not create a checkout session for " + sticker.slug(), e);
        }
    }
}
