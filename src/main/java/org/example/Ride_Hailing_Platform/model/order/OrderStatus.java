package org.example.Ride_Hailing_Platform.model.order;

public enum OrderStatus {
    PENDING,        //待接单
    REJECTED,      //已拒绝
    ACCEPTED,       //已接单
    PICKING_UP,     //前往接客
    IN_PROGRESS,    //行程中
    COMPLETED,      //已完成
    CANCELLED,      //已取消
}
