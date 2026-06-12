package com.ecommerce.nivora.wishlist.service;

import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.exception.dto.request.BadRequestException;
import com.ecommerce.nivora.product.dto.response.ProductImageResponse;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.repository.ProductRepository;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.UserRepository;
import com.ecommerce.nivora.wishlist.entity.Wishlist;
import com.ecommerce.nivora.wishlist.entity.WishlistItem;
import com.ecommerce.nivora.wishlist.repository.WishlistItemRepository;
import com.ecommerce.nivora.wishlist.repository.WishlistRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public List<ProductResponse> getWishlist(Long userId) {
        Wishlist wishlist = getOrCreate(userId);
        return wishlist.getItems().stream()
                .map(item -> toProductResponse(item.getProduct()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addToWishlist(Long userId, Long productId) {
        Wishlist wishlist = getOrCreate(userId);

        if (wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            throw new BadRequestException("Product already in wishlist");
        }

        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        WishlistItem item = WishlistItem.builder()
                .wishlist(wishlist)
                .product(product)
                .build();
        wishlistItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = getOrCreate(userId);
        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getId(), productId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.findByUserId(userId)
                .map(w -> wishlistItemRepository.existsByWishlistIdAndProductId(w.getId(), productId))
                .orElse(false);
    }

    private Wishlist getOrCreate(Long userId) {
        return wishlistRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            return wishlistRepository.save(Wishlist.builder().user(user).build());
        });
    }

    private ProductResponse toProductResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId()).uuid(p.getUuid()).name(p.getName()).slug(p.getSlug())
                .shortDescription(p.getShortDescription()).brand(p.getBrand())
                .basePrice(p.getBasePrice()).comparePrice(p.getComparePrice())
                .isActive(p.getIsActive()).isFeatured(p.getIsFeatured())
                .isNew(p.getIsNew()).isBestSeller(p.getIsBestSeller())
                .ratingAvg(p.getRatingAvg()).ratingCount(p.getRatingCount())
                .images(p.getImages().stream().map(img ->
                    ProductImageResponse.builder().id(img.getId()).url(img.getUrl())
                        .isPrimary(img.getIsPrimary()).sortOrder(img.getSortOrder())
                        .variantId(img.getVariant() != null ? img.getVariant().getId() : null).build()
                ).collect(Collectors.toList()))
                .build();
    }
}
