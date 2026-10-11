package com.shop.service;

import com.shop.model.User;
import com.shop.util.Logger;
import org.springframework.stereotype.Service;
import javax.mail.Session;

public class EmailNotificationService {
    private User user;
    private Logger logger;
    private NotificationService notificationService;
}
