@import java.util.List
@import dev.danvega.store.catalog.Sticker
@param List<Sticker> stickers

@template.layout(title = "Spring Store", content = @`
    <div class="card p-8 md:p-12">
        <div class="flex items-start justify-between gap-6">
            <h1 class="text-5xl md:text-6xl font-extrabold tracking-tight">Spring Store</h1>
            <span class="pill px-4 py-2 shrink-0" style="background:#FFD84D">Stripe test mode</span>
        </div>
        <p class="mt-3 text-lg font-medium opacity-80 max-w-xl">
            Six vinyl stickers, 99&cent; each. One click, straight to Stripe. No cart, nothing
            to fill in, nothing ships.
        </p>

        @if(stickers.isEmpty())
            <div class="mt-10">
                <h2 class="text-3xl font-extrabold">The sheet is empty.</h2>
                <p class="mt-2 text-lg font-medium opacity-80">
                    No stickers are listed right now. Nothing is broken, there is just nothing to buy.
                </p>
            </div>
        @else
            <div class="mt-10 grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
                @for(Sticker sticker : stickers)
                    <div>
                        <div class="tile aspect-square flex items-center justify-center p-5 text-center"
                             style="background:${sticker.colorHex()}">
                            @if(sticker.isDark())
                                <span class="mono text-2xl font-bold" style="color:#7ED957">${sticker.name()}</span>
                            @else
                                <span class="text-2xl font-extrabold leading-tight">${sticker.name()}</span>
                            @endif
                        </div>
                        <form method="post" action="/buy/${sticker.slug()}" class="mt-4">
                            <button type="submit" class="btn-solid w-full py-4 text-lg">
                                Buy &middot; <span class="mono">${sticker.priceDisplay()}</span>
                            </button>
                        </form>
                    </div>
                @endfor
            </div>
        @endif

        <p class="mono mt-10 text-sm opacity-60">
            Payments run through Stripe Checkout in test mode &middot; card 4242 4242 4242 4242
        </p>
    </div>
`)
