package org.example.Ride_Hailing_Platform.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import org.example.Ride_Hailing_Platform.repository.PassengerRepository;
import org.example.Ride_Hailing_Platform.service.PassengerService;
import org.example.Ride_Hailing_Platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {
    @Autowired
    UserService userService;

    @Autowired
    PassengerRepository passengerRepository;

    @Override
    public Passenger createPassenger(String name, String phone, String password, String emergencyContact) {
        User user = userService.createUser(name, phone, password, UserRole.PASSENGER);

        Passenger passenger = new Passenger();
        passenger.setUserId(user.getUserId());
        passenger.setEmergencyContact(emergencyContact);

        return passengerRepository.save(passenger);
    }

    @Override
    public Passenger getPassengerById(Long passengerId) {
        User user = userService.findUserById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("乘客不存在: ID= " + passengerId));
        if (!user.getRole().equals(UserRole.PASSENGER)){
            throw new IllegalArgumentException("该用户不是乘客：ID=" + passengerId);
        }
        return passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("乘客专属信息不存在：ID=" + passengerId));
    }
}
