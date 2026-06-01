# Ride-Hailing Platform

A full-stack ride-hailing platform built with Spring Boot and Vue.js, developed as a course project for Object-Oriented Analysis and Design (OOAD) following the Waterfall methodology.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Backend Setup](#backend-setup)
  - [Frontend Setup](#frontend-setup)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Dynamic Pricing Algorithm](#dynamic-pricing-algorithm)
- [Order State Machine](#order-state-machine)
- [UML Documentation](#uml-documentation)
- [Team](#team)
- [License](#license)

## Overview

This platform connects passengers with drivers for on-demand ride booking. It supports the complete ride lifecycle — from order creation through driver dispatch, trip execution, fare calculation, payment, and review — managed by a seven-state finite state machine. The system integrates the Gaode (AMap) API for route planning and weather data, uses Redis for real-time event broadcasting and caching, and implements a six-factor dynamic pricing algorithm.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Vue.js 3 Frontend                      │
│              (Vite · Pinia · Vue Router · AMap SDK)         │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / WebSocket
┌──────────────────────────▼──────────────────────────────────┐
│                   Spring Boot 3.3 Backend                   │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌──────────────┐   │
│  │Controller│ │  Service │ │ WebSocket │ │    Security  │   │
│  │   Layer  │ │   Layer  │ │  Handler  │ │  (JWT + RBAC)│   │
│  └────┬─────┘ └────┬─────┘ └─────┬─────┘ └──────────────┘   │
│       │            │             │                          │
│  ┌────▼────────────▼─────────────▼──────────────────────┐   │
│  │              JPA/Hibernate (Repository Layer)        │   │
│  └────┬────────────────────────────────────────────┬────┘   │
└───────┼─────────────────────────────────────────────┼───────┘
        │                                             │
┌───────▼────────┐                           ┌────────▼───────┐
│     MySQL      │                           │     Redis      │
│  (Persistent   │                           │  (Cache ·      │
│   Storage)     │                           │   Pub/Sub)     │
└────────────────┘                           └────────────────┘
        │                                             │
        └─────────────┐                   ┌───────────┘
                      │                   │
              ┌───────▼───────────────────▼───────┐
              │       Gaode (AMap) API            │
              │  (Route · Weather · Geocoding)    │
              └───────────────────────────────────┘
```

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend Framework | Spring Boot | 3.3.0 |
| ORM | Spring Data JPA / Hibernate | — |
| Security | Spring Security + JWT (jjwt) | 0.12.3 |
| Database | MySQL | 8.x |
| Cache & Messaging | Redis (Spring Data Redis) | — |
| Real-time | WebSocket (Spring WebSocket) | — |
| API Documentation | SpringDoc OpenAPI (Swagger) | 2.6.0 |
| Build Tool | Maven | — |
| Java | OpenJDK | 17 |
| Frontend Framework | Vue.js | 3.4 |
| State Management | Pinia | 2.1 |
| Routing | Vue Router | 4.3 |
| Build Tool | Vite | 5.4 |
| Map Integration | Gaode Maps JS SDK | 2.0 |

## Features

- **Dual-role user system**: Single-table inheritance with `Passenger` and `Driver` subclasses sharing the `users` table
- **JWT authentication**: Stateless token-based auth with role-based access control
- **Dynamic pricing**: Six-factor algorithm (base fare + distance + duration + time surcharge + congestion + weather)
- **Order state machine**: Seven states with strictly validated transitions
- **Real-time communication**: Redis Pub/Sub for event broadcasting, WebSocket for client push notifications
- **Map integration**: Gaode (AMap) API for route planning, distance/duration estimation, and weather queries
- **Balance-based payment**: Virtual wallet with fare deduction and payment confirmation
- **Trip tracking**: Live location updates during ride
- **Review system**: Post-trip rating and comments

## Project Structure

```
Ride_Hailing_Platform/
├── src/main/java/org/example/ridehailing/
│   ├── common/                    # Shared response wrapper (ApiResponse)
│   ├── config/                    # Spring configurations
│   │   ├── DatabaseConfig.java
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── WebSocketConfig.java
│   ├── controller/                # REST API controllers
│   │   ├── AuthController.java
│   │   ├── DriverController.java
│   │   ├── OrderController.java
│   │   ├── PaymentController.java
│   │   ├── PricingController.java
│   │   ├── ReviewController.java
│   │   └── SystemController.java
│   ├── dto/                       # Data Transfer Objects
│   ├── exception/                 # Global exception handling
│   ├── filter/                    # JWT authentication filter
│   ├── model/                     # JPA entity classes
│   │   ├── order/                 # Order, OrderStatus, OrderType
│   │   ├── payment/               # Payment, PaymentStatus
│   │   ├── review/                # Review
│   │   ├── trip/                  # Trip, TripStatus, Location (Value Object)
│   │   └── user/                  # User (abstract), Passenger, Driver, UserRole
│   ├── repository/                # Spring Data JPA repositories
│   ├── service/                   # Business logic layer
│   │   ├── amap/                  # Gaode API integration
│   │   ├── cache/                 # Redis cache services
│   │   ├── impl/                  # Service implementations
│   │   ├── pricing/               # Dynamic pricing engine
│   │   ├── pubsub/                # Redis Pub/Sub service
│   │   └── (interfaces)          # Service contracts
│   ├── util/                      # Utility classes (JwtUtil, GeoUtil)
│   └── websocket/                 # WebSocket handler
├── src/main/resources/
│   └── schema.sql                 # Database initialization script
├── frontend/                      # Vue.js 3 frontend
│   ├── src/
│   │   ├── components/shared/     # Reusable components (DriverCard, PhoneFrame)
│   │   ├── composables/           # Composable functions (useWebSocket)
│   │   ├── router/                # Route definitions (passenger, driver)
│   │   ├── stores/                # Pinia stores (auth, driver, order)
│   │   └── views/
│   │       ├── passenger/         # Passenger views (Booking, Tracking, Orders, Profile)
│   │       └── driver/            # Driver views (Home, OrderDetail, History, Profile)
│   ├── vite.config.js
│   └── package.json
├── uml/                           # UML diagram sources (PlantUML)
├── report/                        # IEEE-format project report (LaTeX)
└── pom.xml
```

## Getting Started

### Prerequisites

- **Java 17** (OpenJDK recommended)
- **Maven 3.8+**
- **Node.js 18+** and **npm**
- **MySQL 8.x** running on `localhost:3306`
- **Redis** running on `localhost:6379`
- **Gaode (AMap) API Key** — [Apply here](https://lbs.amap.com/)

### Backend Setup

1. **Create the MySQL database**:

   ```sql
   CREATE DATABASE ride_hailing_platform
   DEFAULT CHARACTER SET utf8mb4
   DEFAULT COLLATE utf8mb4_unicode_ci;
   ```

2. **Configure application properties**:

   Create `src/main/resources/application.properties`:

   ```properties
   # MySQL
   spring.datasource.url=jdbc:mysql://localhost:3306/ride_hailing_platform?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=your_password
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

   # JPA
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

   # Redis
   spring.data.redis.host=localhost
   spring.data.redis.port=6379

   # JWT
   jwt.secret=your-jwt-secret-key-at-least-256-bits-long
   jwt.expiration=86400000

   # Gaode Map API
   amap.api.key=your_amap_api_key
   ```

3. **Build and run**:

   ```bash
   ./mvnw spring-boot:run
   ```

   The backend starts at `http://localhost:8080`.

   Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

### Frontend Setup

1. **Install dependencies**:

   ```bash
   cd frontend
   npm install
   ```

2. **Start development server**:

   ```bash
   npm run dev
   ```

   The frontend starts at `http://localhost:5173` with API proxy to `localhost:8080`.

3. **Build for production**:

   ```bash
   npm run build
   ```

## API Reference

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login and get JWT | No |
| POST | `/api/ride/order` | Create ride order | Yes |
| GET | `/api/ride/order/pending` | List pending orders | Yes |
| GET | `/api/ride/order/{id}` | Get order detail | Yes |
| GET | `/api/ride/order/my` | Get my orders | Yes |
| PUT | `/api/ride/order/{id}/accept` | Driver accepts order | Yes |
| PUT | `/api/ride/order/{id}/arrive` | Driver arrives at pickup | Yes |
| PUT | `/api/ride/order/{id}/start` | Start trip | Yes |
| PUT | `/api/ride/order/{id}/complete` | Complete trip | Yes |
| POST | `/api/ride/order/{id}/pay` | Initiate payment | Yes |
| POST | `/api/ride/order/{id}/confirm-pay` | Confirm payment | Yes |
| PUT | `/api/ride/order/{id}/cancel` | Cancel order | Yes |
| GET | `/api/ride/order/estimate-price` | Estimate fare | Yes |
| GET | `/api/driver/online` | Set driver online | Yes |
| GET | `/api/driver/offline` | Set driver offline | Yes |
| POST | `/api/review` | Submit review | Yes |
| GET | `/api/payment/balance` | Get wallet balance | Yes |

Full interactive documentation is available via Swagger UI at runtime.

## Database Schema

The system uses five tables with InnoDB engine and utf8mb4 charset:

### users (Single-Table Inheritance)

| Column | Type | Description |
|--------|------|-------------|
| user_id | BIGINT AUTO_INCREMENT | PK |
| role | VARCHAR(31) | Discriminator: PASSENGER / DRIVER |
| name | VARCHAR(100) | User name |
| phone | VARCHAR(20) UNIQUE | Phone number |
| password | VARCHAR(255) | BCrypt hashed password |
| rating | DOUBLE DEFAULT 5.0 | User rating (0–5) |
| emergency_contact | VARCHAR(100) | Emergency contact (Passenger) |
| ride_count | INT DEFAULT 0 | Ride count (Passenger) |
| license_number | VARCHAR(50) | Driver license (Driver) |
| is_online | BOOLEAN DEFAULT FALSE | Online status (Driver) |
| vehicle_type | VARCHAR(50) | Vehicle type (Driver) |
| vehicle_plate | VARCHAR(20) | License plate (Driver) |

### orders

| Column | Type | Description |
|--------|------|-------------|
| order_id | BIGINT AUTO_INCREMENT | PK |
| passenger_id | BIGINT | FK → users |
| driver_id | BIGINT | FK → users |
| pickup_location / destination | VARCHAR(255) | Address text |
| pickup_lat/lng, dest_lat/lng | DOUBLE | Coordinates |
| distance | DOUBLE | Estimated distance (km) |
| estimated_fare / actual_fare | DOUBLE | Fare amounts |
| base_fare / distance_fare / duration_fare | DOUBLE | Fare breakdown |
| surcharges_json | TEXT | Surcharge details (JSON) |
| status | VARCHAR(20) | Order status (see state machine) |
| type | VARCHAR(20) | Order type (STANDARD) |

### trips

| Column | Type | Description |
|--------|------|-------------|
| trip_id | BIGINT AUTO_INCREMENT | PK |
| order_id | BIGINT UNIQUE | FK → orders |
| pickup/destination/current (address + lat + lng) | — | Three embedded locations |
| actual_distance / actual_fare | DOUBLE | Actual trip data |
| status | VARCHAR(20) | Trip status |

### payments

| Column | Type | Description |
|--------|------|-------------|
| payment_id | BIGINT AUTO_INCREMENT | PK |
| order_id | BIGINT UNIQUE | FK → orders |
| amount | DOUBLE | Payment amount |
| status | VARCHAR(20) | UNPAID / PAID |
| payment_method | VARCHAR(50) | Payment method |

### reviews

| Column | Type | Description |
|--------|------|-------------|
| review_id | BIGINT AUTO_INCREMENT | PK |
| order_id | BIGINT | FK → orders |
| reviewer_id / reviewed_id | BIGINT | FK → users |
| rating | INT | Score (1–5) |
| comment | TEXT | Review text |

## Dynamic Pricing Algorithm

The fare is calculated using a six-factor model:

```
Subtotal = Base Fare + Distance Fare + Duration Fare
Final Fare = min(Subtotal × Total Multiplier, Subtotal × Max Cap)
```

| Factor | Rate / Multiplier | Description |
|--------|-------------------|-------------|
| Base Fare | ¥8.00 | Fixed starting price |
| Distance Fare | ¥2.00/km | Based on AMap route distance |
| Duration Fare | ¥0.50/min | Based on AMap estimated duration |
| Peak Hours | ×1.3 | 7:00–9:00, 17:00–20:00 (weekdays) |
| Night | ×1.4 | 23:00–6:00 |
| Weekend | ×1.1 | Saturday & Sunday |
| Holiday | ×1.1 | Chinese public holidays |
| Congestion Level 2 | ×1.2 | Slow traffic |
| Congestion Level 3 | ×1.5 | Congested |
| Congestion Level 4 | ×2.0 | Severe congestion |
| Bad Weather | ×1.3 | Rain, snow, fog, etc. |
| **Max Cap** | **×2.5** | Total multiplier cannot exceed 2.5× |

Multipliers are applied multiplicatively. The final fare is capped at 2.5× the subtotal.

## Order State Machine

```
PENDING ──accept──▶ ACCEPTED ──arrive──▶ PICKING_UP ──start──▶ IN_PROGRESS ──complete──▶ COMPLETED
   │                   │                                              │
   │                cancel                                         cancel
   │                   │                                              │
   ▼                   ▼                                              ▼
REJECTED           CANCELLED                                      CANCELLED
```

| State | Description |
|-------|-------------|
| PENDING | Order created, waiting for driver |
| REJECTED | No driver available (terminal) |
| ACCEPTED | Driver accepted the order |
| PICKING_UP | Driver en route to pickup point |
| IN_PROGRESS | Trip in progress |
| COMPLETED | Trip finished, fare calculated (terminal) |
| CANCELLED | Cancelled by passenger or driver (terminal) |

State transitions are validated in the service layer to prevent illegal transitions.

## UML Documentation

PlantUML source files are located in the `uml/` directory:

| Diagram | File | Description |
|---------|------|-------------|
| Class Diagram | `class-diagram.puml` | Domain model with entities, interfaces, and services |
| Use Case Diagram | `use-case-diagram.puml` | Passenger, Driver, Administrator use cases |
| Sequence Diagram (Part 1) | `sequence-diagram-1.puml` | Driver online → Order dispatch → Arrival |
| Sequence Diagram (Part 2) | `sequence-diagram-2.puml` | Trip start → Payment → Review → Offline |
| State Diagram | `state-diagram.puml` | Order state machine |
| Activity Diagram | `activity-diagram.puml` | Swimlane ride booking process |
| Component Diagram | `component-diagram.puml` | System component decomposition |
| Deployment Diagram | `deployment-diagram.puml` | Physical deployment architecture |
| Package Diagram | `package-diagram.puml` | Architecture-level package structure |

## License

This project is developed for educational purposes as part of the OOAD course at Wenzhou-Kean University.
