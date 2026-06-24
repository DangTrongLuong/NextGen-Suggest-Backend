package com.ecommerce.nivora.chat.service;

import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import com.ecommerce.nivora.chat.dto.request.ChatRequest;
import com.ecommerce.nivora.chat.dto.response.ChatResponse;
import com.ecommerce.nivora.chat.dto.response.FastApiChatResponse;
import com.ecommerce.nivora.product.dto.response.ProductImageResponse;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.dto.response.ProductVariantResponse;
import com.ecommerce.nivora.product.service.ProductService;
import com.ecommerce.nivora.recommendation.FastApiProductResult;
import com.ecommerce.nivora.security.utils.SecurityUtils;
import com.ecommerce.nivora.user.entity.User;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final RestTemplate restTemplate;
    private final ProductService productService;
    private final SecurityUtils securityUtils;

    private static final Pattern SLUG_PATTERN = Pattern.compile("[^a-z0-9]+");

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    public ChatResponse getChatResponse(ChatRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            return ChatResponse.builder()
                    .response("Lịch sử trò chuyện trống. Tôi có thể giúp gì cho bạn?")
                    .suggestedProducts(Collections.emptyList())
                    .build();
        }

        try {
            // 1. Lấy thông tin User hiện tại nếu đã đăng nhập (không ném lỗi nếu là Guest)
            User currentUser = null;
            try {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    currentUser = securityUtils.getCurrentUser();
                }
            } catch (Exception e) {
                // Khách vãng lai
            }

            // 2. Lấy Session ID — dùng getSession(false) để tránh tạo session mới không cần thiết
            String sessionId = "anonymous-session";
            try {
                org.springframework.web.context.request.ServletRequestAttributes attr =
                    (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
                if (attr != null && attr.getRequest() != null) {
                    jakarta.servlet.http.HttpServletRequest req = attr.getRequest();
                    // Ưu tiên: header custom > session đã tồn tại > IP
                    String xSessionId = req.getHeader("X-Session-Id");
                    if (xSessionId != null && !xSessionId.isBlank()) {
                        sessionId = xSessionId;
                    } else {
                        jakarta.servlet.http.HttpSession existing = req.getSession(false);
                        sessionId = (existing != null) ? existing.getId() : req.getRemoteAddr();
                    }
                }
            } catch (Exception e) {
                // Bỏ qua — context không có sẵn (e.g. background thread)
            }

            // 3. Chuẩn bị Request Payload gửi sang FastAPI
            Map<String, Object> apiRequest = new HashMap<>();
            apiRequest.put("messages", request.getMessages());

            if (currentUser != null) {
                apiRequest.put("user_id", String.valueOf(currentUser.getId()));
                
                Map<String, Object> profile = new HashMap<>();
                profile.put("name", currentUser.getName());
                profile.put("email", currentUser.getEmail());
                profile.put("age", currentUser.getAge());
                profile.put("gender", currentUser.getGender());
                profile.put("occupation", currentUser.getOccupation());
                profile.put("income", currentUser.getIncome());
                profile.put("marital_status", currentUser.getMaritalStatus());
                
                apiRequest.put("user_profile", profile);
            } else {
                apiRequest.put("session_id", sessionId);
            }

            String targetUrl = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/chat")
                    .build()
                    .toUriString();

            log.info("Forwarding personalized chat request to FastAPI: {}", targetUrl);

            FastApiChatResponse apiResponse = restTemplate.postForObject(targetUrl, apiRequest, FastApiChatResponse.class);

            if (apiResponse == null) {
                return ChatResponse.builder()
                        .response("Xin lỗi, hệ thống AI hiện tại không phản hồi. Vui lòng thử lại sau.")
                        .suggestedProducts(Collections.emptyList())
                        .build();
            }

            List<ProductResponse> hydratedProducts = new ArrayList<>();
            if (apiResponse.getSuggestedProducts() != null && !apiResponse.getSuggestedProducts().isEmpty()) {
                List<String> skus = apiResponse.getSuggestedProducts().stream()
                        .map(FastApiProductResult::getProductId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                Map<String, ProductResponse> dbProductsBySku = productService.getActiveProductsBySkus(skus);
                log.info("Hydrated {}/{} semantic chatbot suggested products from database", dbProductsBySku.size(), skus.size());

                for (FastApiProductResult res : apiResponse.getSuggestedProducts()) {
                    ProductResponse dbProduct = dbProductsBySku.get(res.getProductId());
                    if (dbProduct != null) {
                        // Thiết lập điểm số và lý do gợi ý cho sản phẩm chatbot
                        dbProduct.setRecommendationScore(res.getScore());
                        dbProduct.setRecommendationReason(res.getReason() != null ? res.getReason() : "Gợi ý từ trợ lý AI");
                        hydratedProducts.add(dbProduct);
                    } else {
                        hydratedProducts.add(mapFromFastApi(res));
                    }
                }
            }

            return ChatResponse.builder()
                    .response(apiResponse.getResponse())
                    .suggestedProducts(hydratedProducts)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch response from chatbot FastAPI", e);
            return ChatResponse.builder()
                    .response("Đã xảy ra lỗi khi kết nối với Trợ lý AI: " + e.getMessage())
                    .suggestedProducts(Collections.emptyList())
                    .build();
        }
    }

    private ProductResponse mapFromFastApi(FastApiProductResult res) {
        long fakeId = Math.abs((long) res.getProductId().hashCode());
        String leafCat = getLeafCategory(res.getCategory());
        String slug = SLUG_PATTERN.matcher(leafCat.toLowerCase()).replaceAll("-");

        return ProductResponse.builder()
                .id(fakeId)
                .uuid(res.getProductId())
                .sku(res.getProductId())
                .name(res.getTitle())
                .shortDescription(res.getDescription() != null && res.getDescription().length() > 490
                        ? res.getDescription().substring(0, 490) + "..." : res.getDescription())
                .description(res.getDescription())
                .basePrice(res.getPrice() != null ? BigDecimal.valueOf(res.getPrice()) : BigDecimal.ZERO)
                .comparePrice(res.getPrice() != null ? BigDecimal.valueOf(res.getPrice() * 1.2) : BigDecimal.ZERO)
                .stock(100)
                .ratingAvg(BigDecimal.valueOf(4.5))
                .ratingCount(12)
                .isActive(true)
                .isNew(false)
                .isFeatured(false)
                .isBestSeller(false)
                .imageUrl(res.getImageUrl())
                .category(CategoryResponse.builder()
                        .name(leafCat)
                        .slug(slug)
                        .build())
                .images(res.getImageUrl() != null ? List.of(ProductImageResponse.builder()
                        .id(fakeId)
                        .url(res.getImageUrl())
                        .isPrimary(true)
                        .sortOrder(0)
                        .build()) : Collections.emptyList())
                .variants(List.of(ProductVariantResponse.builder()
                        .id(fakeId)
                        .sku(res.getProductId())
                        .name(res.getTitle())
                        .price(res.getPrice() != null ? BigDecimal.valueOf(res.getPrice()) : BigDecimal.ZERO)
                        .comparePrice(res.getPrice() != null ? BigDecimal.valueOf(res.getPrice() * 1.2) : BigDecimal.ZERO)
                        .stock(100)
                        .availableStock(100)
                        .isActive(true)
                        .build()))
                .build();
    }

    private String getLeafCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return "Other";
        }
        String[] parts = categoryStr.split(">");
        return parts[parts.length - 1].trim();
    }
}
