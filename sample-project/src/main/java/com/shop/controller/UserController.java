package com.shop.controller;

import com.shop.service.UserService;
import com.shop.model.User;
import com.shop.util.Validator;
import org.springframework.web.bind.annotation.RestController;

public class UserController {
    private UserService userService;
    private User user;
    private Validator validator;
}
