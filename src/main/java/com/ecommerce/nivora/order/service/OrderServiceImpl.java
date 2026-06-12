package com.ecommerce.nivora.order.service;

import com.ecommerce.nivora.cart.entity.Cart;
import com.ecommerce.nivora.cart.entity.CartItem;
import com.ecommerce.nivora.cart.repository.CartItemRepository;
import com.ecommerce.nivora.cart.repository.CartRepository;
import com.ecommerce.nivora.cart.service.CartService;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.exception.dto.request.BadRequestException;
import com.ecommerce.nivora.order.dto.request.CheckoutRequest;
import com.ecommerce.nivora.order.dto.response.OrderItemResponse;
import com.ecommerce.nivora.order.dto.response.OrderResponse;
import com.ecommerce.nivora.order.dto.response.ShippingAddressResponse;
import com.ecommerce.nivora.order.entity.Order;
import com.ecommerce.nivora.order.entity.OrderItem;
import com.ecommerce.nivora.order.entity.ShippingAddress;
import com.ecommerce.nivora.order.repository.OrderRepository;
import com.ecommerce.nivora.payment.dto.response.PaymentResponse;
import com.ecommerce.nivora.payment.entity.Payment;
import com.ecommerce.nivora.product.entity.Inventory;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.entity.ProductVariant;
import com.ecommerce.nivora.product.repository.InventoryRepository;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.UserRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final CartService cartService;

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @Override
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Validate stock and build order items
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();
            Inventory inventory = inventoryRepository.findByVariantId(variant.getId())
                    .orElseThrow(() -> new BadRequestException("Inventory not found for: " + variant.getSku()));

            if (inventory.getAvailableQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for: " + variant.getName());
            }

            Product product = variant.getProduct();
            BigDecimal itemTotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            String imageUrl = variant.getImageUrl();
            if (imageUrl == null && product.getImages() != null && !product.getImages().isEmpty()) {
                imageUrl = product.getImages().get(0).getUrl();
            }

            orderItems.add(OrderItem.builder()
                    .variant(variant)
                    .product(product)
                    .productName(product.getName())
                    .variantName(variant.getName())
                    .sku(variant.getSku())
                    .imageUrl(imageUrl)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .discountAmount(BigDecimal.ZERO)
                    .totalPrice(itemTotal)
                    .build());
        }

        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("100")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("9.99");
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.08"));
        BigDecimal totalAmount = subtotal.add(shippingFee).add(taxAmount);

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .status(Order.Status.PENDING)
                .subtotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(shippingFee)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .notes(request.getNotes())
                .build();

        // Set order reference on items
        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        // Shipping address snapshot
        ShippingAddress shippingAddress = ShippingAddress.builder()
                .order(order)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .countryCode(request.getCountryCode())
                .build();
        order.setShippingAddress(shippingAddress);

        // Payment record
        Payment payment = Payment.builder()
                .order(order)
                .user(user)
                .method(request.getPaymentMethod())
                .status(Payment.Status.PENDING)
                .amount(totalAmount)
                .currency(order.getCurrency())
                .build();
        order.setPayment(payment);

        orderRepository.save(order);

        // Reserve inventory
        for (CartItem cartItem : cart.getItems()) {
            Inventory inv = inventoryRepository.findByVariantId(cartItem.getVariant().getId()).get();
            inv.setReservedQuantity(inv.getReservedQuantity() + cartItem.getQuantity());
            inventoryRepository.save(inv);
        }

        // Clear cart
        cartService.clearCart(userId);

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return PageResponse.of(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to this user");
        }
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        Order.Status newStatus = Order.Status.valueOf(status.toUpperCase());
        order.setStatus(newStatus);

        switch (newStatus) {
            case CONFIRMED -> order.setConfirmedAt(LocalDateTime.now());
            case PACKING -> order.setPackedAt(LocalDateTime.now());
            case SHIPPING -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> {
                order.setDeliveredAt(LocalDateTime.now());
                // Deduct inventory on delivery
                order.getItems().forEach(item -> {
                    inventoryRepository.findByVariantId(item.getVariant().getId()).ifPresent(inv -> {
                        inv.setQuantity(inv.getQuantity() - item.getQuantity());
                        inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - item.getQuantity()));
                        inventoryRepository.save(inv);
                    });
                });
                // Mark payment completed
                if (order.getPayment() != null) {
                    order.getPayment().setStatus(Payment.Status.COMPLETED);
                    order.getPayment().setPaidAt(LocalDateTime.now());
                }
            }
            default -> {}
        }

        orderRepository.save(order);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to this user");
        }

        if (order.getStatus() == Order.Status.SHIPPING ||
            order.getStatus() == Order.Status.DELIVERED ||
            order.getStatus() == Order.Status.CANCELLED) {
            throw new BadRequestException("Cannot cancel order with status: " + order.getStatus());
        }

        order.setStatus(Order.Status.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledReason(reason);

        // Release reserved inventory
        order.getItems().forEach(item -> {
            inventoryRepository.findByVariantId(item.getVariant().getId()).ifPresent(inv -> {
                inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - item.getQuantity()));
                inventoryRepository.save(inv);
            });
        });

        orderRepository.save(order);
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        return PageResponse.of(orderRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(String status, Pageable pageable) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return PageResponse.of(orderRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse));
        }
        Order.Status enumStatus = Order.Status.valueOf(status.toUpperCase());
        return PageResponse.of(
                orderRepository.findByStatusOrderByCreatedAtDesc(enumStatus, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toResponse(order);
    }

    // ── Helpers ──────────────────────────────────────────

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "ORD-" + date + "-" + String.format("%04d", SEQ.getAndIncrement());
    }

    private OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus().name().toLowerCase())
                .subtotal(o.getSubtotal())
                .discountAmount(o.getDiscountAmount())
                .shippingFee(o.getShippingFee())
                .taxAmount(o.getTaxAmount())
                .totalAmount(o.getTotalAmount())
                .currency(o.getCurrency())
                .notes(o.getNotes())
                .userId(o.getUser() != null ? o.getUser().getId() : null)
                .customerName(o.getUser() != null ? o.getUser().getName() : null)
                .customerEmail(o.getUser() != null ? o.getUser().getEmail() : null)
                .items(o.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()))
                .shippingAddress(o.getShippingAddress() != null ? toShippingResponse(o.getShippingAddress()) : null)
                .payment(o.getPayment() != null ? toPaymentResponse(o.getPayment()) : null)
                .createdAt(o.getCreatedAt())
                .confirmedAt(o.getConfirmedAt())
                .shippedAt(o.getShippedAt())
                .deliveredAt(o.getDeliveredAt())
                .cancelledAt(o.getCancelledAt())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem i) {
        return OrderItemResponse.builder()
                .id(i.getId())
                .productId(i.getProduct().getId())
                .variantId(i.getVariant().getId())
                .productName(i.getProductName())
                .variantName(i.getVariantName())
                .sku(i.getSku())
                .imageUrl(i.getImageUrl())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .totalPrice(i.getTotalPrice())
                .build();
    }

    private ShippingAddressResponse toShippingResponse(ShippingAddress s) {
        return ShippingAddressResponse.builder()
                .id(s.getId())
                .recipientName(s.getRecipientName())
                .phone(s.getPhone())
                .addressLine1(s.getAddressLine1())
                .addressLine2(s.getAddressLine2())
                .city(s.getCity())
                .state(s.getState())
                .postalCode(s.getPostalCode())
                .countryCode(s.getCountryCode())
                .trackingNumber(s.getTrackingNumber())
                .carrier(s.getCarrier())
                .build();
    }

    private PaymentResponse toPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .method(p.getMethod())
                .status(p.getStatus().name().toLowerCase())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .gatewayTransactionId(p.getGatewayTransactionId())
                .paidAt(p.getPaidAt())
                .build();
    }
}
