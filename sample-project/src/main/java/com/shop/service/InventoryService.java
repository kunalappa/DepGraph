package com.shop.service;

import com.shop.repository.ProductRepository;
import com.shop.model.Product;
import com.shop.util.Logger;
import org.springframework.stereotype.Service;
import java.util.Map;

public class InventoryService {
    private ProductRepository productRepository;
    private Product product;
    private Logger logger;
    private OrderService orderService;
}
