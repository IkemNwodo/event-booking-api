# Event Booking API

A RESTful Event Booking API built with Spring Boot, Java 17, and H2 database. This backend application enables creating events with capacity limits, booking seats at events, cancelling bookings, and retrieving event/booking details.

---

## 🛠️ Technology Stack
* **Java 17+** (LTS)
* **Spring Boot 3.3.0** (Spring Web, Spring Data JPA, Jakarta Validation)
* **H2 Database** (In-memory, persistent per runtime session)
* **Lombok** (Code generation/boilerplate reduction)
* **Springdoc OpenAPI (Swagger UI)** (Interactive API documentation)
* **Apache Maven** (Via Maven Wrapper `mvnw`)

---

## 🚀 How to Build and Run

### 1. Build the Project
To compile the code and build the executable JAR, run:
```bash
./mvnw clean package
```

### 2. Run the Tests
To run all unit and integration tests:
```bash
./mvnw test
```

### 3. Run the Application
To start the Spring Boot web application:
```bash
./mvnw spring-boot:run
```
The application will start on `http://localhost:8080`.

---

## 📖 API Documentation & UI

### 1. Swagger UI (OpenAPI 3.0)
You can view, interact with, and test all API endpoints directly through Swagger UI:
* **Swagger URL**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI Spec Json**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### 2. H2 Database Console
To view the database tables and data:
* **H2 Console URL**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
* **JDBC URL**: `jdbc:h2:mem:eventdb`
* **User Name**: `sa`
* **Password**: `password`

---

## 📌 API Endpoints

### Event Management
| Method | Endpoint | Description |
|---|---|---|
| **POST** | `/events` | Create a new event with seat capacity. Event date must be in the future. |
| **GET** | `/events` | List all events (supports pagination: `page`, `size`). |
| **GET** | `/events/{id}` | Get detailed information about a specific event. |

### Booking Management
| Method | Endpoint | Description |
|---|---|---|
| **POST** | `/events/{id}/bookings` | Book a seat for an attendee. Checks capacity, status, and duplicate emails. |
| **GET** | `/events/{id}/bookings` | List all bookings for a specific event (supports pagination: `page`, `size`). |
| **DELETE** | `/bookings/{id}` | Cancel a booking by ID and free up a seat. |

---

## 🧠 Design Decisions

1. **Concurrency Protection**:
   * To prevent race conditions (e.g. multiple users booking the last seat concurrently and causing overbooking), the service locks the Event database row during booking and cancellation using JPA **Pessimistic Write Locking** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
2. **Duplicate Bookings Prevention**:
   * The business rule states: *The same email address cannot book the same event more than once*.
   * In addition to service-level checks, I added a unique database constraint in the `bookings` table on `(event_id, attendee_email)`. This ensures that even under clustered server instances or rapid double-clicks, no duplicate booking can ever bypass validation.
3. **Status Reopening**:
   * If an event is closed because all seats are booked (status becomes `CLOSED`), cancelling a booking will automatically set the status back to `OPEN` since seats are freed.
4. **Clean Serialization**:
   * Enabled `@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)` in the application runner class to avoid Spring Boot's warnings about serializing paginated results as-is, ensuring a stable response format for paginated queries.
