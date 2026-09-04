@template.layout(title = "Checkout cancelled - Spring Store", content = @`
    <div class="card p-8 md:p-12">
        <span class="pill inline-block px-4 py-2" style="background:#FFD84D">Checkout cancelled</span>

        <h1 class="mt-6 text-4xl md:text-5xl font-extrabold">Nothing was charged.</h1>

        <p class="mt-4 text-lg font-medium opacity-80 max-w-2xl">
            You closed Stripe before finishing checkout. Your cart still has everything in it.
        </p>

        <div class="mt-8 flex flex-wrap gap-4">
            <a href="/cart" class="btn-solid inline-block px-8 py-4 text-lg">Back to your cart</a>
            <a href="/" class="btn-hollow inline-block px-8 py-4 text-lg">Keep shopping</a>
        </div>
    </div>
`)
