package com.shop;

import com.shop.config.AppConfig;
import com.shop.controller.OrderController;
import com.shop.controller.ProductController;
import com.shop.controller.UserController;
import org.springframework.boot.SpringApplication;

public class App {
    private AppConfig appConfig;
    private OrderController orderController;
    private ProductController productController;
    private UserController userController;
}
