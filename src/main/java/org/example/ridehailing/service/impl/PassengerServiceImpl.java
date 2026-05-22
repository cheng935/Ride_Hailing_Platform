package org.example.ridehailing.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.exception.BusinessException;
import org.example.ridehailing.model.user.Passenger;
import org.example.ridehailing.model.user.User;
import org.example.ridehailing.model.user.UserRole;
import org.example.ridehailing.repository.PassengerRepository;
import org.example.ridehailing.service.PassengerService;
import org.example.ridehailing.service.UserService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final UserService userService;
    private final PassengerRepository passengerRepository;

    @Override
    public Passenger createPassenger(String name, String phone, String password, String emergencyContact) {
        User user = userService.createUser(name, phone, password, UserRole.PASSENGER);

        Passenger passenger = (Passenger) user;
        passenger.setEmergencyContact(emergencyContact);

        return passengerRepository.save(passenger);
    }

    @Override
    public Passenger getPassengerById(Long passengerId) {
        User user = userService.findUserById(passengerId)
                .orElseThrow(() -> BusinessException.notFound("乘客不存在: ID= " + passengerId));
        if (!user.getRole().equals(UserRole.PASSENGER)) {
            throw BusinessException.badRequest("该用户不是乘客：ID=" + passengerId);
        }
        return passengerRepository.findById(passengerId)
                .orElseThrow(() -> BusinessException.notFound("乘客专属信息不存在：ID=" + passengerId));
    }
}
