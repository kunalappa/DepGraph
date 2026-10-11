package com.shop.service;

import com.shop.repository.OrderRepository;
import com.shop.model.Order;
import com.shop.model.OrderStatus;
import com.shop.util.DateUtils;
import com.shop.util.Logger;
import org.springframework.stereotype.Service;
import java.util.List;

public class OrderService {
    private OrderRepository orderRepository;
    private Order order;
    private OrderStatus orderStatus;
    private DateUtils dateUtils;
    private Logger logger;
    private InventoryService inventoryService;
    private PaymentService paymentService;
    private NotificationService notificationService;
}
