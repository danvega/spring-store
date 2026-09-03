package dev.danvega.store.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;

public interface StickerRepository extends ListCrudRepository<Sticker, Long> {

    Optional<Sticker> findBySlug(String slug);

    List<Sticker> findAllByOrderBySortOrderAsc();
}
