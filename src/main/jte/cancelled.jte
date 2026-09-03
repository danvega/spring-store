@import dev.danvega.store.catalog.Sticker
@param Sticker sticker

@template.layout(title = "Checkout cancelled - Spring Store", content = @`
    <div class="card p-8 md:p-12">
        <span class="pill inline-block px-4 py-2" style="background:#FFD84D">Checkout cancelled</span>

        <h1 class="mt-6 text-4xl md:text-5xl font-extrabold">Nothing was charged.</h1>

        <p class="mt-4 text-lg font-medium opacity-80 max-w-2xl">
            @if(sticker == null)
                You closed Stripe before finishing checkout. Everything is still on the shelf.
            @else
                You closed Stripe before finishing checkout for
                <span class="mono">${sticker.name()}</span>. The sticker is still on the shelf.
            @endif
        </p>

        <div class="mt-8 flex flex-wrap gap-4">
            @if(sticker != null)
                <form method="post" action="/buy/${sticker.slug()}">
                    <button type="submit" class="btn-solid px-8 py-4 text-lg">Try again</button>
                </form>
            @endif
            <a href="/" class="btn-hollow inline-block px-8 py-4 text-lg">Back to the stickers</a>
        </div>
    </div>
`)
