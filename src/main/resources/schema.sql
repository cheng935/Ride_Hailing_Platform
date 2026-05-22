-- ========================================
-- Ride Hailing Platform Database Schema
-- ========================================

CREATE DATABASE IF NOT EXISTS ride_hailing_platform
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE ride_hailing_platform;

-- ========================================
-- 1. 用户表（单表继承）
-- ========================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    role VARCHAR(31) NOT NULL COMMENT '用户类型（PASSENGER/DRIVER）',
    name VARCHAR(100) NOT NULL COMMENT '用户姓名',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    rating DOUBLE DEFAULT 5.0 COMMENT '评分（0-5）',

    emergency_contact VARCHAR(100) COMMENT '紧急联系人',
    ride_count INT DEFAULT 0 COMMENT '乘车次数',

    license_number VARCHAR(50) COMMENT '驾驶证号',
    is_online BOOLEAN DEFAULT FALSE COMMENT '是否在线',
    vehicle_type VARCHAR(50) COMMENT '车辆类型',
    vehicle_plate VARCHAR(20) COMMENT '车牌号',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_phone (phone),
    INDEX idx_role (role),
    INDEX idx_is_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================
-- 2. 订单表
-- ========================================
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    passenger_id BIGINT COMMENT '乘客ID',
    driver_id BIGINT COMMENT '司机ID',
    pickup_location VARCHAR(255) NOT NULL COMMENT '上车地点',
    pickup_lat DOUBLE COMMENT '上车纬度',
    pickup_lng DOUBLE COMMENT '上车经度',
    destination VARCHAR(255) NOT NULL COMMENT '目的地',
    dest_lat DOUBLE COMMENT '目的地纬度',
    dest_lng DOUBLE COMMENT '目的地经度',
    distance DOUBLE COMMENT '预估距离（公里）',
    estimated_fare DOUBLE COMMENT '预估费用',
    actual_fare DOUBLE COMMENT '实际费用',
    base_fare DOUBLE COMMENT '起步价',
    distance_fare DOUBLE COMMENT '里程费',
    surcharges_json TEXT COMMENT '加价明细JSON',
    is_congestion BOOLEAN DEFAULT FALSE COMMENT '是否高峰期',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
    type VARCHAR(20) NOT NULL DEFAULT 'STANDARD' COMMENT '订单类型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    accept_time DATETIME COMMENT '接单时间',
    cancel_time DATETIME COMMENT '取消时间',
    cancel_reason VARCHAR(255) COMMENT '取消原因',

    payment_status VARCHAR(20) DEFAULT 'UNPAID' COMMENT '支付状态',
    payment_time DATETIME COMMENT '支付时间',

    FOREIGN KEY (passenger_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (driver_id) REFERENCES users(user_id) ON DELETE SET NULL,

    INDEX idx_passenger_id (passenger_id),
    INDEX idx_driver_id (driver_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ========================================
-- 3. 行程表
-- ========================================
CREATE TABLE IF NOT EXISTS trips (
    trip_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '行程ID',
    order_id BIGINT NOT NULL UNIQUE COMMENT '订单ID',

    pickup_address VARCHAR(255) COMMENT '起点地址',
    pickup_latitude DOUBLE COMMENT '起点纬度',
    pickup_longitude DOUBLE COMMENT '起点经度',

    destination_address VARCHAR(255) COMMENT '终点地址',
    destination_latitude DOUBLE COMMENT '终点纬度',
    destination_longitude DOUBLE COMMENT '终点经度',

    current_address VARCHAR(255) COMMENT '当前地址',
    current_latitude DOUBLE COMMENT '当前纬度',
    current_longitude DOUBLE COMMENT '当前经度',

    actual_distance DOUBLE COMMENT '实际行驶距离（公里）',
    actual_fare DOUBLE COMMENT '实际费用',
    rating INT COMMENT '评分（1-5）',
    feedback TEXT COMMENT '评价',
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '行程状态',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,

    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程表';

-- ========================================
-- 4. 评价表
-- ========================================
CREATE TABLE IF NOT EXISTS reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    order_id BIGINT COMMENT '订单ID',
    reviewer_id BIGINT COMMENT '评价人ID',
    reviewed_id BIGINT COMMENT '被评价人ID',
    rating INT NOT NULL COMMENT '评分（1-5）',
    comment TEXT COMMENT '评价内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE SET NULL,
    FOREIGN KEY (reviewer_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (reviewed_id) REFERENCES users(user_id) ON DELETE SET NULL,

    INDEX idx_order_id (order_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_reviewed_id (reviewed_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- ========================================
-- 5. 支付表
-- ========================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '支付ID',
    order_id BIGINT NOT NULL UNIQUE COMMENT '订单ID',
    amount DOUBLE NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态',
    payment_method VARCHAR(50) COMMENT '支付方式',
    transaction_id VARCHAR(100) COMMENT '交易流水号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    payment_time DATETIME COMMENT '支付时间',

    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,

    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付表';
