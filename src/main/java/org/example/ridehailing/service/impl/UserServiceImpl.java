package org.example.ridehailing.service.impl;

import org.example.ridehailing.exception.BusinessException;
import org.example.ridehailing.model.user.Driver;
import org.example.ridehailing.model.user.Passenger;
import org.example.ridehailing.model.user.User;
import org.example.ridehailing.model.user.UserRole;
import org.example.ridehailing.repository.DriverRepository;
import org.example.ridehailing.repository.PassengerRepository;
import org.example.ridehailing.repository.UserRepository;
import org.example.ridehailing.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

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
            throw BusinessException.badRequest("手机号已存在");
        }
        User user;
        String encodedPassword = passwordEncoder.encode(password);
        if (role == UserRole.PASSENGER) {
            user = new Passenger(name, phone, encodedPassword);
        } else if (role == UserRole.DRIVER) {
            user = new Driver(name, phone, encodedPassword);
        } else {
            throw BusinessException.badRequest("不支持的用户角色：" + role);
        }
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    public User login(String phone, String password) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw BusinessException.badRequest("密码错误");
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
