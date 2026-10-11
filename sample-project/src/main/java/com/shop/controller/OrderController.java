package com.shop.controller;

import com.shop.service.OrderService;
import com.shop.model.Order;
import com.shop.util.Validator;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

public class OrderController {
    private OrderService orderService;
    private Order order;
    private Validator validator;
}
