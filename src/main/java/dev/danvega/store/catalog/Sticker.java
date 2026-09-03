package dev.danvega.store.catalog;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("sticker")
public record Sticker(@Id Long id, String slug, String name, int priceCents, String color, int sortOrder) {

    public String priceDisplay() {
        return Money.display(priceCents);
    }

    /** The flat fill for this sticker's tile. One colour per sticker, from the design. */
    public String colorHex() {
        return switch (color) {
            case "green" -> "#7ED957";
            case "orange" -> "#FFB02E";
            case "purple" -> "#A78BFA";
            case "red" -> "#FF7A6B";
            case "blue" -> "#4FC3F7";
            case "ink" -> "#171310";
            default -> "#E8E0CC";
        };
    }

    /** The ink tile is dark, so its label flips to green monospace. */
    public boolean isDark() {
        return "ink".equals(color);
    }
}
