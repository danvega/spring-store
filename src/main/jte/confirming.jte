@import dev.danvega.store.catalog.Sticker
@import dev.danvega.store.order.ConfirmingStage
@import dev.danvega.store.order.PurchaseOrder
@import java.time.Instant
@import java.time.ZoneOffset
@import java.time.format.DateTimeFormatter
@param PurchaseOrder order
@param Sticker sticker
@param ConfirmingStage stage
@param Instant arrivedAt
@param long waitedSeconds
@param String refreshUrl

!{var stamp = DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);}

@template.layout(
    title = stage == ConfirmingStage.NOT_RECORDED ? "Not recorded - Spring Store" : "Confirming - Spring Store",
    head = @`@if(stage.keepRefreshing())<meta http-equiv="refresh" content="2;url=${refreshUrl}">@endif`,
    content = @`
    <div class="card p-8 md:p-12">
        <div class="flex flex-wrap gap-3">
            <span class="pill inline-block px-4 py-2" style="background:#7ED957">Paid at Stripe</span>
            @if(stage == ConfirmingStage.WAITING)
                <span class="pill inline-block px-4 py-2" style="background:#FFD84D">Recording</span>
            @elseif(stage == ConfirmingStage.TAKING_LONGER)
                <span class="pill inline-block px-4 py-2" style="background:#FFD84D">Taking longer</span>
            @else
                <span class="pill inline-block px-4 py-2" style="background:#FFD84D">Not recorded</span>
            @endif
        </div>

        @if(stage == ConfirmingStage.WAITING)
            <h1 class="mt-6 text-4xl md:text-5xl font-extrabold">Confirming your payment.</h1>
            <p class="mt-4 text-lg font-medium opacity-80 max-w-2xl">
                Stripe took the payment. The store is waiting for Stripe to tell it, which
                usually takes a second or two.
            </p>
        @elseif(stage == ConfirmingStage.TAKING_LONGER)
            <h1 class="mt-6 text-4xl md:text-5xl font-extrabold">Still confirming.</h1>
            <p class="mt-4 text-lg font-medium opacity-80 max-w-2xl">
                This is slower than usual. Your payment is safe at Stripe either way, the
                store just has not recorded it yet. You can leave this page open or check
                again.
            </p>
        @else
            <h1 class="mt-6 text-4xl md:text-5xl font-extrabold leading-tight">
                Stripe has the payment. The store does not.
            </h1>
            <p class="mt-4 text-lg font-medium opacity-80 max-w-2xl">
                The payment went through at Stripe, but the store never heard back, so
                there is no order to show. Nothing here is your fault and there is nothing
                for you to fix. This is a test app, so no money moved and nothing was going
                to ship.
            </p>
        @endif

        <div class="mt-8 border-4 border-[color:var(--ink)] rounded-2xl p-5 flex items-center gap-6">
            <div class="tile w-32 h-32 shrink-0 flex items-center justify-center p-3 text-center"
                 style="background:${sticker.colorHex()}">
                @if(sticker.isDark())
                    <span class="mono text-sm font-bold" style="color:#7ED957">${sticker.name()}</span>
                @else
                    <span class="text-sm font-extrabold leading-tight">${sticker.name()}</span>
                @endif
            </div>
            <div>
                <p class="text-2xl md:text-3xl font-extrabold leading-tight">${sticker.name()}</p>
                <p class="mono mt-1 text-xl">${order.amountDisplay()} USD</p>
            </div>
        </div>

        @if(stage == ConfirmingStage.NOT_RECORDED)
            <dl class="mt-8 border-4 border-[color:var(--ink)] rounded-2xl overflow-hidden mono">
                <div class="flex justify-between px-6 py-4">
                    <dt class="opacity-60">Came back from Stripe</dt>
                    <dd class="font-bold">${stamp.format(arrivedAt)}</dd>
                </div>
                <div class="flex justify-between px-6 py-4 border-t-4 border-[color:var(--ink)]">
                    <dt class="opacity-60">Gave up waiting</dt>
                    <dd class="font-bold">${waitedSeconds}s</dd>
                </div>
            </dl>
            <div class="mt-8 flex flex-wrap gap-4">
                <a href="${refreshUrl}" class="btn-solid inline-block px-8 py-4 text-lg">Check one more time</a>
                <a href="/" class="btn-hollow inline-block px-8 py-4 text-lg">Back to the stickers</a>
            </div>
        @else
            <p class="mono mt-8 text-sm opacity-70">waiting ${waitedSeconds}s for stripe webhook</p>
            @if(stage == ConfirmingStage.TAKING_LONGER)
                <a href="${refreshUrl}" class="btn-solid inline-block mt-6 px-8 py-4 text-lg">Check again</a>
            @endif
            <p class="mono mt-8 text-sm opacity-60">
                Test mode. No card was charged and no sticker ships.
            </p>
        @endif
    </div>
`)
