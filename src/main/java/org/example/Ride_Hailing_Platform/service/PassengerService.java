package org.example.Ride_Hailing_Platform.service;

import org.example.Ride_Hailing_Platform.model.user.Passenger;

public interface PassengerService {

    /**
     * 创建乘客--复用 UserService 创建用户后，补充乘客专属信息）
     * @param name
     * @param phone
     * @param passwoed
     * @param emergencyContact
     * @return 乘客对象
     */
    Passenger createPassenger(String name, String phone, String passwoed, String emergencyContact);

    /**
     * 通过ID查询乘客
     * @param PassengerId
     * @return  乘客对象
     */
    Passenger getPassengerById(Long PassengerId);

}
