package com.shop.service;

import com.shop.repository.UserRepository;
import com.shop.model.User;
import com.shop.util.Validator;
import org.springframework.stereotype.Service;
import java.util.Optional;

public class UserService {
    private UserRepository userRepository;
    private User user;
    private Validator validator;
    private NotificationService notificationService;
}
