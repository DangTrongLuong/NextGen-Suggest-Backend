package com.ecommerce.nivora.cart.service;

import com.ecommerce.nivora.cart.dto.request.CartItemRequest;
import com.ecommerce.nivora.cart.dto.response.CartItemResponse;
import com.ecommerce.nivora.cart.dto.response.CartResponse;
import com.ecommerce.nivora.cart.entity.Cart;
import com.ecommerce.nivora.cart.entity.CartItem;
import com.ecommerce.nivora.cart.repository.CartItemRepository;
import com.ecommerce.nivora.cart.repository.CartRepository;
import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.exception.dto.request.BadRequestException;
import com.ecommerce.nivora.product.entity.Inventory;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.entity.ProductVariant;
import com.ecommerce.nivora.product.repository.InventoryRepository;
import com.ecommerce.nivora.product.repository.ProductVariantRepository;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.UserRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant", request.getVariantId()));

        if (!variant.getIsActive()) {
            throw new BadRequestException("This product variant is not available");
        }

        Inventory inventory = inventoryRepository.findByVariantId(variant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory for variant", variant.getId()));

        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: " + inventory.getAvailableQuantity());
        }

        CartItem existing = cartItemRepository.findByCartAndVariant(cart, variant).orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + request.getQuantity();
            if (inventory.getAvailableQuantity() < newQty) {
                throw new BadRequestException("Insufficient stock for requested quantity");
            }
            existing.setQuantity(newQty);
            cartItemRepository.save(existing);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .unitPrice(variant.getPrice())
                    .build();
            cart.getItems().add(item);
        }

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long variantId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));

        CartItem item = cartItemRepository.findByCartAndVariant(cart, variant)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            Inventory inventory = inventoryRepository.findByVariantId(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", variantId));
            if (inventory.getAvailableQuantity() < quantity) {
                throw new BadRequestException("Insufficient stock. Available: " + inventory.getAvailableQuantity());
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long variantId) {
        Cart cart = getOrCreateCart(userId);
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));

        cartItemRepository.findByCartAndVariant(cart, variant).ifPresent(item -> {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        });

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    // ── Helpers ──────────────────────────────────────────

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .subtotal(subtotal)
                .itemCount(items.stream().mapToInt(CartItemResponse::getQuantity).sum())
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        ProductVariant v = item.getVariant();
        Product p = v.getProduct();
        int available = 0;
        if (v.getInventory() != null) available = v.getInventory().getAvailableQuantity();

        String imageUrl = v.getImageUrl();
        if (imageUrl == null && p.getImages() != null && !p.getImages().isEmpty()) {
            imageUrl = p.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(img -> img.getUrl())
                    .orElse(p.getImages().get(0).getUrl());
        }

        return CartItemResponse.builder()
                .id(item.getId())
                .variantId(v.getId())
                .productId(p.getId())
                .productName(p.getName())
                .variantName(v.getName())
                .sku(v.getSku())
                .imageUrl(imageUrl)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .availableStock(available)
                .build();
    }
}
