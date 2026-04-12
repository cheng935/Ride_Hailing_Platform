// src/main/java/org/example/Ride_Hailing_Platform/service/OrderService.java
package org.example.Ride_Hailing_Platform.service;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderStatus;
import org.example.Ride_Hailing_Platform.model.order.OrderType;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import java.util.List;
import java.util.Optional;

/**
 * 订单服务接口 - 定义订单业务契约
 */
public interface OrderService {

    //获取待处理订单（司机视角）
    List<Order> getPendingOrders();

    //获取司机专属待处理订单（带位置过滤）(未实现)
    //List<Order> getPendingOrdersForDriver(Long driverId, double latitude, double longitude);

    /**
     * 创建新订单
     * @param passengerId 乘客ID
     * @param pickupLocation 上车地点
     * @param destination 目的地
     * @param type 订单类型
     * @return 创建的订单
     * @throws IllegalArgumentException 如果乘客不存在或参数无效
     */
    Order createOrder(Long passengerId, String pickupLocation, String destination, OrderType type);

    /**
     * 司机接单
     * @param orderId 订单ID
     * @param driverId 司机ID
     * @return 更新后的订单
     * @throws IllegalStateException 如果订单状态不允许接单
     */
    Order acceptOrder(Long orderId, Long driverId);

    /**
     * 取消订单
     * @param orderId 订单ID
     * @param userId 取消用户ID（乘客或司机）
     * @return 更新后的订单
     * @throws IllegalStateException 如果订单不允许取消
     */
    Order cancelOrder(Long orderId, Long userId);

    /**
     * 根据ID查找订单
     * @param orderId 订单ID
     * @return 订单对象（可选）
     */
    Optional<Order> findOrderById(Long orderId);

    /**
     * 获取用户的进行中订单
     * @param userId 用户ID
     * @param isPassenger 是否为乘客
     * @return 进行中订单列表
     */
    List<Order> getActiveOrders(Long userId, boolean isPassenger);

    /**
     * 获取用户的历史订单
     * @param userId 用户ID
     * @param isPassenger 是否为乘客
     * @return 历史订单列表
     */
    List<Order> getHistoryOrders(Long userId, boolean isPassenger);

    /**
     * 乘客请求行程
     * @param passenger 乘客对象
     * @param pickupLocation 起点地址
     * @param destination 终点地址
     * @return 创建的订单
     * @throws IllegalArgumentException 如果参数无效
     */

    Order requestRide(Passenger passenger, String pickupLocation, String destination);

}
