# LC Study Partner Backend

A Spring Boot REST API for the LC Study Partner application, providing comprehensive backend services for Leaving Certificate exam preparation.

## Features

- **User Authentication**: JWT-based authentication with registration and login
- **Dashboard Analytics**: Study statistics and progress tracking
- **Study Sessions**: Track and manage study sessions with duration and topics
- **Past Papers Management**: Store and retrieve LC exam papers by subject and year
- **RESTful API**: Clean REST endpoints for frontend integration
- **Database Support**: H2 (development) and PostgreSQL (production) support
- **Security**: Spring Security with CORS support for frontend integration

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Security** with JWT authentication
- **Spring Data JPA** for database operations
- **H2 Database** (development) / **PostgreSQL** (production)
- **Maven** for dependency management
- **OpenAPI/Swagger** documentation (planned)

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. Clone the repository
```bash
git clone <repository-url>
cd study-partner-backend
```

2. Run the application
```bash
mvn spring-boot:run
```

The API will be available at: `http://localhost:8080/api`

### Development Database

The application uses H2 in-memory database for development:
- **Console**: http://localhost:8080/api/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: `password`

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login

### Dashboard
- `GET /api/dashboard/stats` - Get user statistics
- `GET /api/dashboard/user` - Get current user info
- `GET /api/dashboard/quick-actions` - Get quick action items

### Health Check
- `GET /api/actuator/health` - Application health status

## Configuration

Key configuration in `application.yml`:

```yaml
# Database
spring.datasource.url: jdbc:h2:mem:testdb

# JWT
jwt.secret: study-partner-secret-key-for-jwt-token-generation-2024
jwt.expiration: 86400000 # 24 hours

# CORS
cors.allowed-origins: http://localhost:3000,http://localhost:8081,http://localhost:8082
```

## Environment Variables

For production deployment, set these environment variables:

- `DATABASE_URL` - PostgreSQL database URL
- `JWT_SECRET` - JWT secret key
- `OPENAI_API_KEY` - OpenAI API key for AI tutoring features

## Frontend Integration

This backend is designed to work with the React TypeScript frontend located in the `your-study-partner` directory. The API provides all endpoints needed for:

- User authentication and session management
- Dashboard statistics and analytics
- Study session tracking
- Past papers management
- AI tutoring integration

## Development

### Project Structure

```
src/main/java/com/studypartner/
├── StudyPartnerApplication.java
├── config/          # Security and CORS configuration
├── controller/      # REST API controllers
├── dto/            # Data Transfer Objects
├── entity/         # JPA entities
├── repository/     # Data access layer
├── security/       # JWT and security components
└── service/        # Business logic layer
```

### Adding New Features

1. Create JPA entities in `entity/` package
2. Add repositories in `repository/` package
3. Implement business logic in `service/` package
4. Create REST endpoints in `controller/` package
5. Add DTOs for request/response in `dto/` package

## Contributing

This is a learning project for LC students. Contributions are welcome!

## License

This project is for educational purposes.