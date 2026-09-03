@import dev.danvega.store.catalog.Sticker
@import dev.danvega.store.order.PurchaseOrder
@import java.time.ZoneOffset
@import java.time.format.DateTimeFormatter
@param PurchaseOrder order
@param Sticker sticker

!{var stamp = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm 'UTC'").withZone(ZoneOffset.UTC);}

@template.layout(title = "Payment received - Spring Store", content = @`
    <div class="card p-8 md:p-12">
        <span class="pill inline-block px-4 py-2" style="background:#7ED957">Payment received</span>

        <div class="mt-8 flex flex-col sm:flex-row gap-8 items-start">
            <div class="tile w-44 h-44 shrink-0 flex items-center justify-center p-4 text-center"
                 style="background:${sticker.colorHex()}">
                @if(sticker.isDark())
                    <span class="mono font-bold" style="color:#7ED957">${sticker.name()}</span>
                @else
                    <span class="font-extrabold leading-tight">${sticker.name()}</span>
                @endif
            </div>
            <div>
                <h1 class="text-4xl md:text-5xl font-extrabold leading-tight">${sticker.name()}</h1>
                <p class="mono mt-2 text-2xl">${order.amountDisplay()} USD paid</p>
            </div>
        </div>

        <dl class="mt-10 border-4 border-[color:var(--ink)] rounded-2xl overflow-hidden mono">
            <div class="flex justify-between px-6 py-4">
                <dt class="opacity-60">Amount paid</dt>
                <dd class="font-bold">${order.amountDisplay()} USD</dd>
            </div>
            <div class="flex justify-between px-6 py-4 border-t-4 border-[color:var(--ink)]">
                <dt class="opacity-60">Order reference</dt>
                <dd class="font-bold">${order.reference()}</dd>
            </div>
            <div class="flex justify-between px-6 py-4 border-t-4 border-[color:var(--ink)]">
                <dt class="opacity-60">Paid</dt>
                <dd class="font-bold">${stamp.format(order.paidAtStripe())}</dd>
            </div>
        </dl>

        <p class="mt-6 text-lg font-medium opacity-80 max-w-2xl">
            Test-mode payment. No card was charged and no sticker ships. Keep the reference
            to match this against the Stripe dashboard.
        </p>

        <a href="/" class="btn-solid inline-block mt-8 px-8 py-4 text-lg">Back to the stickers</a>
    </div>
`)
