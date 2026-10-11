package com.shop.config;

import com.shop.util.Logger;
import com.shop.repository.OrderRepository;
import com.shop.repository.ProductRepository;
import com.shop.repository.UserRepository;
import org.springframework.context.annotation.Configuration;

public class AppConfig {
    private Logger logger;
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
}
