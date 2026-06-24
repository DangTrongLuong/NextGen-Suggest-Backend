package com.ecommerce.nivora.recommendation.service;

import com.ecommerce.nivora.event.repository.UserBehaviorEventRepository;
import com.ecommerce.nivora.order.repository.OrderRepository;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.repository.ProductRepository;
import com.ecommerce.nivora.product.service.ProductService;
import com.ecommerce.nivora.recommendation.FastApiProductResult;
import com.ecommerce.nivora.recommendation.dto.request.FastApiHybridRecommendRequest;
import com.ecommerce.nivora.recommendation.dto.response.FastApiRecommendResponse;
import com.ecommerce.nivora.recommendation.dto.response.HomepageRecommendationResponse;
import com.ecommerce.nivora.recommendation.entity.RecommendationCache;
import com.ecommerce.nivora.recommendation.repository.RecommendationCacheRepository;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.entity.UserAddress;
import com.ecommerce.nivora.user.repository.UserRepository;
import com.ecommerce.nivora.wishlist.repository.WishlistRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RestTemplate restTemplate;
    private final ProductService productService;
    private final RecommendationCacheRepository cacheRepository;
    private final UserBehaviorEventRepository behaviorEventRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final com.ecommerce.nivora.user.repository.UserAddressRepository userAddressRepository;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    @Transactional(readOnly = true)
    public HomepageRecommendationResponse getHomepageRecommendations(String sessionId, Long userId, List<Long> guestViewedIds) {
        log.info("Generating homepage recommendations for user={}, session={}, guestViewedCount={}", 
                userId, sessionId, guestViewedIds != null ? guestViewedIds.size() : 0);

        // 1–4. Run static sections in parallel to reduce homepage latency
        CompletableFuture<List<ProductResponse>> featuredFuture =
                CompletableFuture.supplyAsync(productService::getFeatured);
        CompletableFuture<List<ProductResponse>> bestSellersFuture =
                CompletableFuture.supplyAsync(productService::getBestSellers);
        CompletableFuture<List<ProductResponse>> newArrivalsFuture =
                CompletableFuture.supplyAsync(productService::getNewArrivals);
        CompletableFuture<List<ProductResponse>> trendingFuture =
                CompletableFuture.supplyAsync(() -> {
                    List<ProductResponse> t = getTrendingFromCache("trending_today");
                    return t.isEmpty() ? productService.getTrendingFallback(8) : t;
                });

        CompletableFuture.allOf(featuredFuture, bestSellersFuture, newArrivalsFuture, trendingFuture).join();

        List<ProductResponse> featured = featuredFuture.join();
        List<ProductResponse> bestSellers = bestSellersFuture.join();
        List<ProductResponse> newArrivals = newArrivalsFuture.join();
        List<ProductResponse> trending = trendingFuture.join();

        // 3. Lấy Tiếp tục xem (Recent Viewed)
        List<ProductResponse> recent = getRecentViewed(sessionId, userId, guestViewedIds);

        // 4. Thu thập hồ sơ người dùng (Demographics) và vị trí để cá nhân hóa gợi ý
        Integer age = null;
        String gender = null;
        String occupation = null;
        Double income = null;
        String maritalStatus = null;
        String location = null;
        List<String> locationProductUuids = new ArrayList<>();

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                age = user.getAge();
                gender = user.getGender();
                occupation = user.getOccupation();
                if (user.getIncome() != null) {
                    income = user.getIncome().doubleValue();
                }
                maritalStatus = user.getMaritalStatus();

                // Trích xuất tỉnh thành từ danh sách địa chỉ nhận hàng
                if (user.getAddresses() != null) {
                    for (UserAddress addr : user.getAddresses()) {
                        if (Boolean.TRUE.equals(addr.getIsDefault())) {
                            location = addr.getCity();
                            break;
                        }
                    }
                    if (location == null && !user.getAddresses().isEmpty()) {
                        location = user.getAddresses().get(0).getCity();
                    }
                }
            }

            if (location != null && !location.isBlank()) {
                locationProductUuids = orderRepository.findTopProductsByLocation(location, PageRequest.of(0, 15));
            }
        }

        // 5. Lấy AI Recommendations (Personalized) & You May Need (Cross-sell)
        List<ProductResponse> personal = new ArrayList<>();
        List<ProductResponse> youMayNeed = new ArrayList<>();
        
        try {
            // Chuẩn bị dữ liệu gửi sang FastAPI
            List<String> viewedUuids = recent.stream().map(ProductResponse::getUuid).collect(Collectors.toList());
            List<String> wishlistUuids = getWishlistUuids(userId);
            List<String> purchasedUuids = getPurchasedUuids(userId);
            List<String> popularUuids = trending.stream().map(ProductResponse::getUuid).collect(Collectors.toList());

            String targetUrl = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/recommend/hybrid")
                    .build()
                    .toUriString();

            FastApiHybridRecommendRequest apiRequest = FastApiHybridRecommendRequest.builder()
                    .viewed_product_ids(viewedUuids)
                    .wishlist_product_ids(wishlistUuids)
                    .purchased_product_ids(purchasedUuids)
                    .popular_product_ids(popularUuids)
                    .location_product_ids(locationProductUuids)
                    .user_age(age)
                    .user_gender(gender)
                    .user_occupation(occupation)
                    .user_income(income)
                    .user_marital_status(maritalStatus)
                    .user_location(location)
                    .user_id(userId != null ? String.valueOf(userId) : null)
                    .top_k(12)
                    .exclude_viewed(true)
                    .build();

            log.info("Requesting hybrid recommendations from FastAPI: {}", targetUrl);
            FastApiRecommendResponse apiResponse = restTemplate.postForObject(targetUrl, apiRequest, FastApiRecommendResponse.class);

            if (apiResponse != null && apiResponse.getRecommendations() != null && !apiResponse.getRecommendations().isEmpty()) {
                List<String> recommendedUuids = apiResponse.getRecommendations().stream()
                        .map(FastApiProductResult::getProductId)
                        .collect(Collectors.toList());

                log.info("[Recommendation] userId: {}, cacheKey: recommend:user:{}:homepage, viewHistory: {}, recommendationIds: {}",
                        userId, userId, viewedUuids, recommendedUuids);

                Map<String, ProductResponse> hydratedProducts = productService.getActiveProductsByUuids(recommendedUuids);
                
                // Bản đồ ánh xạ kết quả FastAPI để sao chép score & reason nhanh
                Map<String, FastApiProductResult> apiResultMap = apiResponse.getRecommendations().stream()
                        .collect(Collectors.toMap(FastApiProductResult::getProductId, r -> r, (r1, r2) -> r1));

                List<ProductResponse> allRecs = recommendedUuids.stream()
                        .map(uuid -> {
                            ProductResponse hydrated = hydratedProducts.get(uuid);
                            if (hydrated != null) {
                                FastApiProductResult apiRes = apiResultMap.get(uuid);
                                if (apiRes != null) {
                                    hydrated.setRecommendationScore(apiRes.getScore());
                                    hydrated.setRecommendationReason(apiRes.getReason());
                                }
                            }
                            return hydrated;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // Phân bổ: 8 cái đầu cho Personal, 4 cái sau cho You May Need
                if (allRecs.size() > 8) {
                    personal.addAll(allRecs.subList(0, 8));
                    youMayNeed.addAll(allRecs.subList(8, allRecs.size()));
                } else {
                    personal.addAll(allRecs);
                    // Nếu ít hơn 8, lấy thêm bestSellers bù vào You May Need
                    youMayNeed.addAll(bestSellers.stream()
                            .filter(p -> !personal.contains(p))
                            .limit(4)
                            .collect(Collectors.toList()));
                    for (ProductResponse p : youMayNeed) {
                        if (p.getRecommendationReason() == null) {
                            p.setRecommendationReason("Có thể bạn quan tâm");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch hybrid recommendations from FastAPI", e);
        }

        // Fallbacks nếu FastAPI gặp lỗi hoặc không trả về kết quả
        if (personal.isEmpty()) {
            personal.addAll(featured);
            for (ProductResponse p : personal) {
                p.setRecommendationReason("Sản phẩm nổi bật tuần này");
                p.setRecommendationScore(0.85);
            }
        }
        if (youMayNeed.isEmpty()) {
            youMayNeed.addAll(bestSellers.stream().limit(4).collect(Collectors.toList()));
            for (ProductResponse p : youMayNeed) {
                p.setRecommendationReason("Sản phẩm bán chạy nhất hệ thống");
                p.setRecommendationScore(0.8);
            }
        }

        return HomepageRecommendationResponse.builder()
                .featured(featured)
                .bestSellers(bestSellers)
                .newArrivals(newArrivals)
                .trending(trending)
                .recent(recent)
                .personal(personal)
                .youMayNeed(youMayNeed)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getRecommendationsByLocation(String location, int limit) {
        log.info("Fetching regional recommendations for location: {}, limit: {}", location, limit);
        if (location == null || location.isBlank()) {
            return Collections.emptyList();
        }

        List<String> productUuids = orderRepository.findTopProductsByLocation(location, PageRequest.of(0, limit));
        if (productUuids.isEmpty()) {
            List<ProductResponse> bestSellers = productService.getBestSellers();
            return bestSellers.stream()
                    .limit(limit)
                    .map(p -> {
                        p.setRecommendationReason("💡 Phổ biến tại " + location);
                        p.setRecommendationScore(0.8);
                        return p;
                    })
                    .collect(Collectors.toList());
        }

        Map<String, ProductResponse> hydratedProducts = productService.getActiveProductsByUuids(productUuids);
        return productUuids.stream()
                .map(uuid -> {
                    ProductResponse p = hydratedProducts.get(uuid);
                    if (p != null) {
                        p.setRecommendationReason("💡 Phổ biến tại " + location);
                        p.setRecommendationScore(0.9);
                    }
                    return p;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<ProductResponse> getTrendingFromCache(String recType) {
        Optional<RecommendationCache> cacheOpt = cacheRepository.findByUserIdAndRecType(null, recType);
        if (cacheOpt.isPresent()) {
            String productIdsStr = cacheOpt.get().getProductIds();
            if (productIdsStr != null && !productIdsStr.isBlank()) {
                try {
                    List<Long> ids = Arrays.stream(productIdsStr.split(","))
                            .map(String::trim)
                            .map(Long::parseLong)
                            .collect(Collectors.toList());
                    
                    return productService.getProductsByIds(ids);
                } catch (Exception e) {
                    log.error("Error parsing trending cache product IDs: {}", productIdsStr, e);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<ProductResponse> getRecentViewed(String sessionId, Long userId, List<Long> guestViewedIds) {
        if (userId != null) {
            // Lấy từ user behavior logs ở Database
            List<String> recentUuids = behaviorEventRepository.findRecentViewedProductUuidsByUserId(
                    userId, PageRequest.of(0, 10));
            if (!recentUuids.isEmpty()) {
                Map<String, ProductResponse> hydrated = productService.getActiveProductsByUuids(recentUuids);
                return recentUuids.stream()
                        .map(hydrated::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList(); // Không lấy ké dữ liệu khách vãng lai hoặc session cũ
        }
        
        // Nếu không có trong DB hoặc là Guest User
        if (guestViewedIds != null && !guestViewedIds.isEmpty()) {
            return productService.getProductsByIds(guestViewedIds);
        }

        // Lấy theo session trong DB làm dự phòng
        if (sessionId != null && !sessionId.isBlank()) {
            List<String> sessionUuids = behaviorEventRepository.findRecentViewedProductUuidsBySessionId(
                    sessionId, PageRequest.of(0, 10));
            if (!sessionUuids.isEmpty()) {
                Map<String, ProductResponse> hydrated = productService.getActiveProductsByUuids(sessionUuids);
                return sessionUuids.stream()
                        .map(hydrated::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private List<String> getWishlistUuids(Long userId) {
        if (userId == null) return Collections.emptyList();
        return wishlistRepository.findByUserId(userId)
                .map(w -> w.getItems().stream()
                        .map(item -> item.getProduct().getUuid())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private List<String> getPurchasedUuids(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            return orderRepository.findPurchasedProductUuidsByUserId(userId);
        } catch (Exception e) {
            log.error("Error fetching purchased product UUIDs for user={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLocationBasedRecommendations(Long userId, int limit) {
        log.info("Fetching location-based recommendations for user={}, limit={}", userId, limit);
        
        com.ecommerce.nivora.user.entity.UserAddress defaultAddr = null;
        if (userId != null) {
            defaultAddr = userAddressRepository.findByUserIdAndIsDefaultTrueAndDeletedAtIsNull(userId).orElse(null);
        }
        
        List<ProductResponse> resultList = new ArrayList<>();
        
        if (defaultAddr != null && 
            ((defaultAddr.getWard() != null && !defaultAddr.getWard().isBlank()) || 
             (defaultAddr.getDistrict() != null && !defaultAddr.getDistrict().isBlank()) || 
             (defaultAddr.getProvince() != null && !defaultAddr.getProvince().isBlank()))) {
             
            String ward = defaultAddr.getWard();
            String district = defaultAddr.getDistrict();
            String province = defaultAddr.getProvince();
            
            List<Long> wardPids = new ArrayList<>();
            List<Long> districtPids = new ArrayList<>();
            List<Long> provincePids = new ArrayList<>();
            
            if (ward != null && !ward.isBlank()) {
                orderRepository.findTopProductsByWard(ward, PageRequest.of(0, limit)).forEach(row -> {
                    wardPids.add((Long) row[0]);
                });
            }
            if (district != null && !district.isBlank()) {
                orderRepository.findTopProductsByDistrict(district, PageRequest.of(0, limit)).forEach(row -> {
                    districtPids.add((Long) row[0]);
                });
            }
            if (province != null && !province.isBlank()) {
                orderRepository.findTopProductsByProvince(province, PageRequest.of(0, limit)).forEach(row -> {
                    provincePids.add((Long) row[0]);
                });
            }
            
            // Use LinkedHashSet for O(1) dedup while preserving insertion order
            LinkedHashSet<Long> seenPids = new LinkedHashSet<>();
            Map<Long, String> pidToReason = new HashMap<>();

            for (Long pid : wardPids) {
                if (seenPids.add(pid)) {
                    pidToReason.put(pid, "🔥 Bán chạy tại " + ward);
                }
            }

            for (Long pid : districtPids) {
                if (seenPids.size() >= limit) break;
                if (seenPids.add(pid)) {
                    pidToReason.put(pid, "🔥 Bán chạy tại " + district);
                }
            }

            for (Long pid : provincePids) {
                if (seenPids.size() >= limit) break;
                if (seenPids.add(pid)) {
                    pidToReason.put(pid, "🔥 Bán chạy tại " + province);
                }
            }

            List<Long> orderedPids = new ArrayList<>(seenPids);
            
            if (!orderedPids.isEmpty()) {
                List<ProductResponse> hydrated = productService.getProductsByIds(orderedPids);
                Map<Long, ProductResponse> productMap = hydrated.stream()
                        .collect(Collectors.toMap(ProductResponse::getId, p -> p));
                        
                for (Long pid : orderedPids) {
                    ProductResponse p = productMap.get(pid);
                    if (p != null) {
                        p.setRecommendationReason(pidToReason.get(pid));
                        p.setRecommendationScore(0.95);
                        resultList.add(p);
                    }
                }
            }
        }
        
        if (resultList.size() < limit) {
            List<ProductResponse> nationwide = getTrendingFromCache("trending_today");
            if (nationwide.isEmpty()) {
                nationwide = productService.getTrendingFallback(limit);
            }
            
            Set<Long> resultIds = resultList.stream()
                    .map(ProductResponse::getId)
                    .collect(Collectors.toSet());
            for (ProductResponse p : nationwide) {
                if (resultList.size() >= limit) break;
                if (resultIds.add(p.getId())) {
                    p.setRecommendationReason("🔥 Sản phẩm nổi bật toàn quốc");
                    p.setRecommendationScore(0.85);
                    resultList.add(p);
                }
            }
        }
        
        return resultList;
    }
}
