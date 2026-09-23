@import java.util.List
@import java.util.Map
@import dev.danvega.store.catalog.Sticker
@param List<Sticker> stickers
@param long itemCount
@param Map<Long, Integer> inCart

@template.layout(title = "Spring Store", content = @`
    <div class="card p-8 md:p-12">
        <div class="flex items-start justify-between gap-6">
            <div>
                <h1 class="text-5xl md:text-6xl font-extrabold tracking-tight">Spring Store</h1>
                <p class="mt-3 text-lg font-medium opacity-80 max-w-xl">
                    Six vinyl stickers, 99&cent; each. Add what you want, then check out once.
                    Nothing ships.
                </p>
            </div>
            <div class="flex flex-col items-end gap-3 shrink-0">
                <span class="pill px-4 py-2" style="background:#FFD84D">Stripe test mode</span>
                <a href="/cart" class="btn-solid px-5 py-3 text-base">
                    Cart &middot; <span class="mono">${itemCount}</span>
                </a>
            </div>
        </div>

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
                    !{var held = inCart.getOrDefault(sticker.id(), 0);}
                    <div>
                        <div class="tile aspect-square flex items-center justify-center p-5 text-center"
                             style="background:${sticker.colorHex()}">
                            @if(sticker.isDark())
                                <span class="mono text-2xl font-bold" style="color:#7ED957">${sticker.name()}</span>
                            @else
                                <span class="text-2xl font-extrabold leading-tight">${sticker.name()}</span>
                            @endif
                        </div>
                        <form method="post" action="/cart/add/${sticker.slug()}" class="mt-4">
                            @if(held > 0)
                                <button type="submit" class="btn-hollow w-full py-4 text-lg">
                                    In cart &middot; <span class="mono">${held}</span>
                                </button>
                            @else
                                <button type="submit" class="btn-solid w-full py-4 text-lg">
                                    Add &middot; <span class="mono">${sticker.priceDisplay()}</span>
                                </button>
                            @endif
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
