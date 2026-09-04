@template.layout(title = "Your cart is empty - Spring Store", content = @`
    <div class="card p-8 md:p-12">
        <h1 class="text-4xl md:text-5xl font-extrabold">Your cart is empty.</h1>
        <p class="mt-4 text-lg font-medium opacity-80 max-w-xl">
            You have not picked anything yet. The stickers are still on the sheet.
        </p>
        <div class="mt-8 flex gap-3">
            <div class="w-24 h-24 rounded-2xl" style="border:3px dashed #B8AE93"></div>
            <div class="w-24 h-24 rounded-2xl" style="border:3px dashed #B8AE93"></div>
            <div class="w-24 h-24 rounded-2xl" style="border:3px dashed #B8AE93"></div>
        </div>
        <a href="/" class="btn-solid inline-block mt-8 px-8 py-4 text-lg">Back to the stickers</a>
    </div>
`)
