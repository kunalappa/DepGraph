package com.shop.controller;

import com.shop.service.InventoryService;
import com.shop.model.Product;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

public class ProductController {
    private InventoryService inventoryService;
    private Product product;
}
