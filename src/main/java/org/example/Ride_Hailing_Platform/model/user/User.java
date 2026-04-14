package org.example.Ride_Hailing_Platform.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
//配置单表继承策略，指定 role 作为区分子类的字段
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "role",
        discriminatorType = DiscriminatorType.STRING
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;//子类 Driver/Passenger 复用该主键

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(nullable = false)
    private String password;

    private Double rating = 5.0; //默认评分

    @Enumerated(EnumType.STRING)
    private UserRole role; //PASSENGER, DRIVER
}
