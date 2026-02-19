package com.example.orderservice.service;

import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.RefundRequestResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String BUYER_ID = "buyer-1";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID OFFER_ID = UUID.randomUUID();

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private com.example.orderservice.client.OfferServiceClient offerServiceClient;
    @Mock
    private com.example.orderservice.client.PaymentServiceClient paymentServiceClient;
    @Mock
    private RefundRequestService refundRequestService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                offerServiceClient,
                paymentServiceClient,
                refundRequestService
        );
    }

    @Test
    void getOrdersByBuyer_returnsListFromRepository() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setBuyerId(BUYER_ID);
        order.setOfferId(OFFER_ID);
        order.setOfferTitleSnapshot("Test offer");
        order.setAmountCents(999);
        order.setQuantity(1);
        order.setCurrency("USD");
        order.setStatus("PAID");
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());

        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(BUYER_ID)).thenReturn(List.of(order));

        List<OrderResponse> list = orderService.getOrdersByBuyer(BUYER_ID);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(ORDER_ID);
        assertThat(list.get(0).getOfferTitle()).isEqualTo("Test offer");
        assertThat(list.get(0).getStatus()).isEqualTo("PAID");
    }

    @Test
    void getOrderById_returnsOrderWhenFound() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setBuyerId(BUYER_ID);
        order.setOfferId(OFFER_ID);
        order.setOfferTitleSnapshot("Test offer");
        order.setAmountCents(999);
        order.setQuantity(1);
        order.setCurrency("USD");
        order.setStatus("PAID");
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());

        when(orderRepository.findByIdAndBuyerId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order));
        when(refundRequestService.getByOrderAndBuyer(ORDER_ID, BUYER_ID)).thenReturn(null);

        OrderResponse response = orderService.getOrderById(ORDER_ID, BUYER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(ORDER_ID);
        assertThat(response.getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(response.getRefundRequest()).isNull();
    }

    @Test
    void getOrderById_includesRefundRequestWhenPresent() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setBuyerId(BUYER_ID);
        order.setOfferId(OFFER_ID);
        order.setOfferTitleSnapshot("Test offer");
        order.setAmountCents(999);
        order.setQuantity(1);
        order.setCurrency("USD");
        order.setStatus("PAID");
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());

        RefundRequestResponse refund = new RefundRequestResponse();
        refund.setStatus("PENDING");

        when(orderRepository.findByIdAndBuyerId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order));
        when(refundRequestService.getByOrderAndBuyer(ORDER_ID, BUYER_ID)).thenReturn(refund);

        OrderResponse response = orderService.getOrderById(ORDER_ID, BUYER_ID);

        assertThat(response.getRefundRequest()).isNotNull();
        assertThat(response.getRefundRequest().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getOrderById_throwsWhenNotFound() {
        when(orderRepository.findByIdAndBuyerId(ORDER_ID, BUYER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID, BUYER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void cancelOrder_updatesStatusToCancelled() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setBuyerId(BUYER_ID);
        order.setOfferId(OFFER_ID);
        order.setOfferTitleSnapshot("Test offer");
        order.setAmountCents(999);
        order.setQuantity(1);
        order.setCurrency("USD");
        order.setStatus("PENDING_PAYMENT");
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());

        when(orderRepository.findByIdAndBuyerId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(ORDER_ID, BUYER_ID);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(order);
        assertThat(order.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_throwsWhenNotPendingPayment() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setBuyerId(BUYER_ID);
        order.setStatus("PAID");

        when(orderRepository.findByIdAndBuyerId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, BUYER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only orders awaiting payment can be cancelled");
        verify(orderRepository, never()).save(any());
    }
}
