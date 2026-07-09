# Analytics Service - Quick Start Guide

## Prerequisites

- Java 17+ installed
- Maven 3.8+ or Maven wrapper available
- MySQL 8.0+ running on localhost:3306
- Other CivicDesk services running or accessible

## Quick Start

### 1. Build the Service

```bash
cd c:\Project\CivicDesk_Micro\civicDesk\analytics-service

# Using Maven wrapper (recommended)
..\civicdesk\mvnw clean package -DskipTests

# Or using Maven directly if installed
mvn clean package -DskipTests
```

**Expected Output:**
```
[INFO] Building analytics-service 1.0.0
...
[INFO] Building jar: target/analytics-service-1.0.0.jar
[INFO] BUILD SUCCESS
```

### 2. Run the Service

**Option A: Using built JAR**
```bash
java -jar target/analytics-service-1.0.0.jar
```

**Option B: Using Maven**
```bash
..\civicdesk\mvnw spring-boot:run
```

**Option C: Using Docker**
```bash
docker build -t civicdesk/analytics-service:1.0.0 .
docker run -p 8088:8088 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/civicdesk_analytics \
  -e APP_AUTH_SERVICE_URL=http://auth-service:8081 \
  -e APP_GRIEVANCE_SERVICE_URL=http://grievance-service:8085 \
  civicdesk/analytics-service:1.0.0
```

### 3. Verify Service is Running

Service should be available at: `http://localhost:8088`

**Check health:**
```bash
curl http://localhost:8088/actuator/health
```

**Expected output:**
```json
{
  "status": "UP"
}
```

### 4. Access Swagger UI

**Direct Swagger:**
- http://localhost:8088/civicDesk/swagger-ui.html

**Through API Gateway:**
- http://localhost:9090/swagger-ui.html
- Select "Analytics Service" from dropdown

### 5. Generate Your First Report

**Step 1: Get JWT Token from Auth Service**
```bash
curl -X POST http://localhost:9090/civicDesk/iam/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@civicdesk.com",
    "password": "password123"
  }'
```

**Expected response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {...}
  }
}
```

**Step 2: Generate Report**
```bash
curl -X POST http://localhost:9090/civicDesk/analytics/reports \
  -H "Authorization: Bearer <token-from-step-1>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "GRIEVANCE",
    "departmentId": "DPT01",
    "fromDate": "2026-01-01T00:00:00",
    "toDate": "2026-12-31T23:59:59"
  }'
```

**Expected response:**
```json
{
  "success": true,
  "message": "Report is ready",
  "data": {
    "reportId": "550e8400-e29b-41d4-a716-446655440000",
    "reportType": "GRIEVANCE",
    "generatedDate": "2026-07-07T16:45:00",
    "status": "GENERATED"
  }
}
```

### 6. Download Report

```bash
curl -X GET http://localhost:9090/civicDesk/analytics/reports/550e8400-e29b-41d4-a716-446655440000/download \
  -H "Authorization: Bearer <token>" \
  -o report.xlsx
```

### 7. Verify Audit Logging

```bash
curl -X GET "http://localhost:9090/civicDesk/audit/auditLogs?module=ANALYTICS&action=GENERATE_REPORT" \
  -H "Authorization: Bearer <token>"
```

**Expected response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "audit-id-123",
        "userId": "USR001",
        "action": "GENERATE_REPORT",
        "module": "ANALYTICS",
        "ipAddress": "127.0.0.1",
        "timestamp": "2026-07-07T16:45:00"
      }
    ],
    "pageNo": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

## Configuration

### Required Services

These services must be running for full functionality:

| Service | Port | Purpose |
|---------|------|---------|
| MySQL | 3306 | Database |
| Auth Service | 8081 | JWT tokens & audit logging |
| Grievance Service | 8085 | Grievance analytics data |
| API Gateway | 9090 | External access (optional but recommended) |

### Environment Variables

Override in `application.properties` or via environment variables:

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/civicdesk_analytics
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root

# JWT Secret (must match auth-service)
APP_JWT_SECRET=civicdesk_hs256_secret_key_minimum_32_characters_required

# Service URLs
APP_AUTH_SERVICE_URL=http://localhost:8081
APP_GRIEVANCE_SERVICE_URL=http://localhost:8085

# Server
SERVER_PORT=8088

# Logging
LOGGING_LEVEL_COM_CIVICDESK_ANALYTICS=DEBUG
```

## Troubleshooting

### Service fails to start - Database connection error

**Solution:**
1. Verify MySQL is running: `mysql -u root -p -e "SELECT 1;"`
2. Create database if needed: `CREATE DATABASE civicdesk_analytics;`
3. Check credentials in `application.properties`

```bash
# Test connection:
mysql -u root -p -h localhost civicdesk_analytics -e "SHOW TABLES;"
```

### Service fails to connect to Auth Service

**Solution:**
1. Verify Auth Service is running on port 8081: `curl http://localhost:8081/actuator/health`
2. Update `app.auth-service.url` in `application.properties`
3. Check firewall/network connectivity

### No reports generated - Grievance Service connection error

**Solution:**
1. Verify Grievance Service is running on port 8085: `curl http://localhost:8085/actuator/health`
2. Update `app.grievance-service.url` in `application.properties`
3. Check service-to-service network connectivity
4. Reports will still generate without grievance data (metrics will be empty)

### JWT Token validation fails

**Solution:**
1. Verify JWT secret matches between auth-service and analytics-service
2. Ensure token is passed with `Authorization: Bearer <token>` header
3. Check token expiration: `curl http://localhost:8081/civicDesk/iam/auth/verify?token=<token>`

### Port 8088 already in use

**Solution:**
1. Kill process using port: `netstat -ano | findstr :8088`
2. Or change port in `application.properties`: `server.port=8089`

## Logs Location

Logs are stored in:
```
logs/analytics-service/analytics-service.log
```

**View real-time logs:**
```bash
# Using tail (if available)
tail -f logs/analytics-service/analytics-service.log

# Using PowerShell
Get-Content -Path logs/analytics-service/analytics-service.log -Tail 50 -Wait
```

## Performance Tips

### Optimize for Production

1. **Disable SQL echo:**
   ```properties
   spring.jpa.show-sql=false
   ```

2. **Enable query caching:**
   ```properties
   spring.jpa.properties.hibernate.cache.use_second_level_cache=true
   ```

3. **Set appropriate database pool:**
   ```properties
   spring.datasource.hikari.maximum-pool-size=20
   spring.datasource.hikari.minimum-idle=5
   ```

4. **Use connection timeouts:**
   ```properties
   spring.datasource.hikari.connection-timeout=20000
   spring.datasource.hikari.idle-timeout=300000
   ```

### Scale the Service

For multiple instances behind a load balancer:
1. Use externalized configuration (Spring Cloud Config)
2. Share database across instances
3. Use sticky sessions or distributed sessions
4. Monitor instance health

## Development

### Running Tests

```bash
# Run all tests
..\civicdesk\mvnw test

# Run specific test class
..\civicdesk\mvnw test -Dtest=ReportControllerTest

# Run with coverage
..\civicdesk\mvnw test jacoco:report
```

### Debug Mode

```bash
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/analytics-service-1.0.0.jar
```

### Local Development with IDE

1. Open project in IDE (Eclipse, IntelliJ, VS Code)
2. Configure run configuration:
   - Main class: `com.civicdesk.analytics.AnalyticsServiceApplication`
   - VM options: `-Xmx512m`
   - Environment variables: Set as needed
3. Run with debug support

## API Documentation

Full API documentation available after service starts:

- **Swagger JSON:** `http://localhost:8088/v3/api-docs`
- **Swagger UI:** `http://localhost:8088/civicDesk/swagger-ui.html`
- **ReDoc:** (if configured) `http://localhost:8088/redoc.html`

## Common Tasks

### Check Service Status
```bash
curl http://localhost:8088/actuator/health
```

### View Application Info
```bash
curl http://localhost:8088/actuator/info
```

### List Available Endpoints
```bash
curl http://localhost:8088/actuator/mappings | jq '.contexts.application.mappings.dispatcherServlets.dispatcherServlet[]'
```

### View JVM Metrics
```bash
curl http://localhost:8088/actuator/metrics
curl http://localhost:8088/actuator/metrics/jvm.memory.used
```

### Clear Reports (soft delete)
```bash
# Mark report as deleted (doesn't remove from database)
curl -X DELETE http://localhost:8088/civicDesk/analytics/reports/{reportId} \
  -H "Authorization: Bearer <token>"
```

## Support & Resources

- **Service Documentation:** See [README.md](./README.md)
- **Integration Details:** See [ANALYTICS_SERVICE_INTEGRATION.md](../ANALYTICS_SERVICE_INTEGRATION.md)
- **Before/After Comparison:** See [ANALYTICS_BEFORE_AFTER.md](../ANALYTICS_BEFORE_AFTER.md)
- **API Gateway Config:** See [api-gateway/src/main/resources/application.yml](../api-gateway/src/main/resources/application.yml)
- **Auth Service Audit:** See [auth-service/README.md](../auth-service/README.md)

## Next Steps

1. ✅ Build and run analytics-service
2. ✅ Verify Swagger documentation
3. ✅ Generate sample report
4. ✅ Verify audit logging
5. ✅ Run integration tests
6. ✅ Deploy to staging environment
7. ✅ Load testing and performance tuning
8. ✅ Production deployment

---

**Service Status:** ✅ Ready for Development/Testing  
**Build:** ✅ Successful (0 errors)  
**Database:** ✅ Auto-created on startup  
**Security:** ✅ JWT-based authentication  
**Audit:** ✅ Integrated with auth-service
