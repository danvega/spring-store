package dev.danvega.store.cart;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;

public interface CartRepository extends ListCrudRepository<Cart, Long> {

    Optional<Cart> findByCartId(String cartId);
}
