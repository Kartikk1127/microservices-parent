package com.Kartikey.orderservice.service;

import com.Kartikey.orderservice.dto.InventoryResponse;
import com.Kartikey.orderservice.dto.OrderLineItemsDto;
import com.Kartikey.orderservice.dto.OrderRequest;
import com.Kartikey.orderservice.model.Order;
import com.Kartikey.orderservice.model.OrderLineItems;
import com.Kartikey.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsList().stream().map(
                this::mapToDto
        ).toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(
                OrderLineItems::getSkuCode
        ).toList();

        //call inventory service and place order if product is in stock
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(
                InventoryResponse::getIsInStock
        );

        if(Boolean.TRUE.equals(allProductsInStock)){
            orderRepository.save(order);
        }
        else {
            throw new IllegalArgumentException("Product is not in stock. Please try again later");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
