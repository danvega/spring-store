package dev.danvega.store.checkout;

import java.util.List;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

@Component
class StripeCheckoutGateway implements StripeGateway {

    private final StripeClient stripe;

    StripeCheckoutGateway(StripeClient stripe) {
        this.stripe = stripe;
    }

    @Override
    public CheckoutSession start(List<Line> lines, String orderReference, String successUrl, String cancelUrl) {
        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(orderReference);

        for (var line : lines) {
            params.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity((long) line.quantity())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount((long) line.unitPriceCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(line.name())
                                    .build())
                            .build())
                    .build());
        }

        try {
            var session = stripe.checkout().sessions().create(params.build());
            return new CheckoutSession(session.getId(), session.getUrl());
        }
        catch (StripeException e) {
            throw new CheckoutFailedException("Stripe would not create a checkout session for " + orderReference, e);
        }
    }
}
