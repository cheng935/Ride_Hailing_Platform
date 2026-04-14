package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import org.example.Ride_Hailing_Platform.service.OrderService;
import org.example.Ride_Hailing_Platform.service.PassengerService;
import org.example.Ride_Hailing_Platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//核心注解说明：
// @RestController = @Controller + @ResponseBody：告诉Spring这是控制器，返回值直接转JSON给前端
// @RequestMapping("/api/passengers")：所有该控制器的请求都以/api/passengers开头（比如/api/passengers/1/orders）
// @RequiredArgsConstructor：Lombok自动生成构造方法，注入final修饰的依赖（替代@Autowired）
@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {
    private final OrderService orderService;
    private final UserService userService;
    private final PassengerService passengerService;


    /**
     * 乘客叫车接口（前端调用这个接口发起叫车请求）
     * @param passengerId
     * @param startAddress
     * @param endAddress
     * @param isCongestion  是否拥堵（用于计算额外费用 1.2倍）
     * @return  创建好的订单
     */
    @PostMapping("/{passengerId}/orders") //POST请求，路径：/api/passengers/1/orders
    public ResponseEntity<?> requestRide(
            @PathVariable Long passengerId,//从URL路径中取passengerId（比如1）
            @RequestParam String startAddress,// 从请求参数中取startAddress（startAddress=天安门）
            @RequestParam String endAddress,
            @RequestParam Boolean isCongestion
            ) {
        try {
            // 步骤1：校验并获取乘客（单表继承下的正确写法）
            // 1.1 先查User，确保用户存在且是乘客
            User user = userService.findUserById(passengerId)
                    .orElseThrow(() -> new RuntimeException("乘客不存在：ID=" + passengerId));
            if (!(user instanceof Passenger)) {
                throw new RuntimeException("该用户不是乘客（userId: " + passengerId + "）");
            }
            Passenger passenger = (Passenger) user;

            //步骤2：调用Service层核心业务逻辑（Controller只转发，不写逻辑）
            Order order = orderService.requestRide(passenger, startAddress, endAddress, isCongestion);
            //步骤3：返回响应给前端（200 OK + 订单JSON）
            return ResponseEntity.ok(order);
        } catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }


    }

    /**
     * 查询乘客所有订单的接口
     * @param passengerId
     * @return 乘客的订单列表（JSON格式）
     */
    @GetMapping("/{passengerId}/orders") //POST请求，路径：/api/passengers/1/orders
    public ResponseEntity<List<Order>> getPassengerOrders(@PathVariable Long passengerId) {
        passengerService.getPassengerById(passengerId);//校验乘客信息，（.getPassengerById()内置校验，故不写逻辑）
        List<Order> orders = orderService.getOrdersByPassengerId(passengerId);
        return ResponseEntity.ok(orders);
    }
}
