@import java.util.List
@import dev.danvega.store.web.LineView
@param List<LineView> lines
@param String totalDisplay
@param int itemCount

@template.layout(title = "Your cart - Spring Store", content = @`
    <div class="card p-8 md:p-12">
        <div class="flex items-start justify-between gap-6">
            <h1 class="text-4xl md:text-5xl font-extrabold">Your cart</h1>
            <span class="pill px-4 py-2 shrink-0" style="background:#FFD84D">Stripe test mode</span>
        </div>
        <p class="mt-3 text-lg font-medium opacity-80">
            ${itemCount} ${itemCount == 1 ? "sticker" : "stickers"}. Nothing ships, nothing is charged.
        </p>

        <div class="mt-8 border-4 border-[color:var(--ink)] rounded-2xl overflow-hidden">
            @for(int i = 0; i < lines.size(); i++)
                !{var line = lines.get(i); var sticker = line.sticker();}
                <div class="flex flex-wrap items-center gap-5 p-5 ${i > 0 ? "border-t-4 border-[color:var(--ink)]" : ""}">
                    <div class="tile w-20 h-20 shrink-0 flex items-center justify-center p-2 text-center"
                         style="background:${sticker.colorHex()}">
                        @if(sticker.isDark())
                            <span class="mono text-xs font-bold" style="color:#7ED957">${sticker.name()}</span>
                        @else
                            <span class="text-xs font-extrabold leading-tight">${sticker.name()}</span>
                        @endif
                    </div>
                    <div class="flex-1 min-w-40">
                        <p class="text-xl font-extrabold leading-tight">${sticker.name()}</p>
                        <p class="mono text-sm opacity-60">${line.unitPriceDisplay()} each</p>
                    </div>
                    <form method="post" action="/cart/quantity/${sticker.id()}" class="flex items-center gap-2">
                        <button type="submit" name="quantity" value="${line.quantity() - 1}" class="qty">&minus;</button>
                        <span class="mono text-lg font-bold w-6 text-center">${line.quantity()}</span>
                        <button type="submit" name="quantity" value="${line.quantity() + 1}" class="qty">+</button>
                    </form>
                    <p class="mono text-lg font-bold w-20 text-right">${line.lineTotalDisplay()}</p>
                    <form method="post" action="/cart/remove/${sticker.id()}">
                        <button type="submit" class="mono text-sm opacity-60 underline">remove</button>
                    </form>
                </div>
            @endfor
        </div>

        <div class="mt-6 flex items-baseline justify-between">
            <p class="text-2xl font-extrabold">Total</p>
            <p class="mono text-3xl font-bold">${totalDisplay}</p>
        </div>
        <p class="mono mt-2 text-sm opacity-60">The store adds this up, not your browser.</p>

        <div class="mt-8 flex flex-wrap gap-4">
            <form method="post" action="/cart/checkout">
                <button type="submit" class="btn-solid px-8 py-4 text-lg">
                    Checkout &middot; <span class="mono">${totalDisplay}</span>
                </button>
            </form>
            <a href="/" class="btn-hollow inline-block px-8 py-4 text-lg">Keep shopping</a>
        </div>
    </div>
`)
