package org.example.Ride_Hailing_Platform;

import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.example.Ride_Hailing_Platform.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RideHailingPlatformApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUsers() {//测试创建用户
        User user = userService.createUser(
                "测试用户",
                "13886208531",
                "123456",
                UserRole.PASSENGER
        );


        Assertions.assertNotNull(user);//验证用户不为null
        Assertions.assertEquals("测试用户", user.getName());//验证姓名
        Assertions.assertNotNull(user.getUserId());//验证ID自增成功

        //验证数据库插入了数据
        User savedUser = userRepository.findById(user.getUserId()).orElse(null);
        Assertions.assertNotNull(savedUser);
    }

    @Test
    void createDuplicateUsers(){//测试创建重复用户警告
        //第一次注册
        userService.createUser(
                "重复用户",
                "13581430991",
                "123456",
                UserRole.PASSENGER
        );

        //第二次注册(重复用户)
        //预计抛出IllegalArgumentException("手机号已存在")
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(
                    "重复用户",
                    "13581430991",
                    "123456",
                    UserRole.PASSENGER
            );
        });

    }

}
