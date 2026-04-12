// src/main/java/org/example/Ride_Hailing_Platform/service/UserService.java
package org.example.Ride_Hailing_Platform.service;

import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import java.util.Optional;

/**
 * 用户服务接口 - 定义业务契约
 * @author YourName
 * @since 2026-04-12
 */
public interface UserService {
    /**
     * 创建新用户
     * @param name 用户姓名
     * @param phone 手机号
     * @param password 密码
     * @param role 用户角色
     * @return 创建的用户对象
     * @throws IllegalArgumentException 如果手机号已存在
     */
    User createUser(String name, String phone, String password, UserRole role);

    //登录
    /**
     * 用户登录
     * @param phone 手机号
     * @param password 密码
     * @return 登录的用户对象
     * @throws IllegalArgumentException 如用户不存在或密码错误
     */
    User login(String phone, String password);

    /**
     * 通过手机号查找用户
     * @param phone 手机号
     * @return 用户对象（可选）
     */
    Optional<User> findUserByPhone(String phone);

    /**
     * 通过ID查找用户
     * @param userId 用户ID
     * @return 用户对象（可选）
     */
    Optional<User> findUserById(Long userId);
}
