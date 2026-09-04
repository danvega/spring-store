@import java.util.List
@import dev.danvega.store.web.LineView
@param List<LineView> lines
@param String totalDisplay

<div class="border-4 border-[color:var(--ink)] rounded-2xl overflow-hidden">
    @for(int i = 0; i < lines.size(); i++)
        !{var line = lines.get(i); var sticker = line.sticker();}
        <div class="flex items-center gap-5 p-5 ${i > 0 ? "border-t-4 border-[color:var(--ink)]" : ""}">
            <div class="tile w-16 h-16 shrink-0 flex items-center justify-center p-2 text-center"
                 style="background:${sticker.colorHex()}">
                @if(sticker.isDark())
                    <span class="mono text-[10px] font-bold" style="color:#7ED957">${sticker.name()}</span>
                @else
                    <span class="text-[10px] font-extrabold leading-tight">${sticker.name()}</span>
                @endif
            </div>
            <div class="flex-1">
                <p class="text-lg font-extrabold leading-tight">${sticker.name()}</p>
                <p class="mono text-sm opacity-60">${line.unitPriceDisplay()} &times; ${line.quantity()}</p>
            </div>
            <p class="mono text-lg font-bold">${line.lineTotalDisplay()}</p>
        </div>
    @endfor
    <div class="flex items-center justify-between p-5 border-t-4 border-[color:var(--ink)]">
        <p class="text-xl font-extrabold">Total</p>
        <p class="mono text-2xl font-bold">${totalDisplay}</p>
    </div>
</div>
