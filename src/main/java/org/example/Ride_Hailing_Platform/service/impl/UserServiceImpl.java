// src/main/java/org/example/Ride_Hailing_Platform/service/impl/UserServiceImpl.java
package org.example.Ride_Hailing_Platform.service.impl;

import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import org.example.Ride_Hailing_Platform.repository.DriverRepository;
import org.example.Ride_Hailing_Platform.repository.PassengerRepository;
import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.example.Ride_Hailing_Platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * UserService接口的实现类
 * Service注解标记为Spring组件
 * Transactional管理事务
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public User createUser(String name, String phone, String password, UserRole role) {
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("手机号已存在");
        }
        User user;
        String encodedPassword = passwordEncoder.encode(password);
        if (role == UserRole.PASSENGER) {
            user = new Passenger(name, phone, encodedPassword);
        } else if (role == UserRole.DRIVER) {
            user = new Driver(name, phone, encodedPassword);
        } else {
            throw new RuntimeException("不支持的用户角色：" + role);
        }
        user.setRole(role);
        return userRepository.save(user);
    }
    
    @Override
    public User login(String phone, String password){
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new RuntimeException("密码错误");
        }

        return user;
    }
    
    @Override
    public Optional<User> findUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Override
    public Optional<User> findUserById(Long userId) {
        return userRepository.findById(userId);
    }
}