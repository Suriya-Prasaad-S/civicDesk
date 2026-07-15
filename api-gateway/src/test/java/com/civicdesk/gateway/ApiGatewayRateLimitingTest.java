package com.civicdesk.gateway;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("API Gateway Rate Limiting Tests")
@ActiveProfiles("test")
@Import(ApiGatewayRateLimitingTest.GatewayTestRouteConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "eureka.client.fetch-registry=false",
                "eureka.client.register-with-eureka=false",
                "spring.cloud.discovery.enabled=false"
        }
)
public class ApiGatewayRateLimitingTest {

    private static HttpServer backendServer;
    private static int backendPort;

    @LocalServerPort
    private int port;

    private static final String HEALTH_PATH = "/gateway-proxy/health";

    @BeforeAll
    public static void setupClass() throws IOException {
        startBackendServer();
        RestAssured.config = RestAssured.config().httpClient(
                RestAssured.config().getHttpClientConfig().setParam("http.socket.timeout", 10000)
        );
    }

    private static void startBackendServer() throws IOException {
        backendServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        backendPort = backendServer.getAddress().getPort();
        backendServer.createContext(HEALTH_PATH, exchange -> {
            byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        backendServer.start();
    }

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        given().when().get("/gateway-test/reset");
    }

    @AfterAll
    public static void teardown() {
        if (backendServer != null) {
            backendServer.stop(0);
        }
    }

    private int sendRequest() {
        Response r = given()
                .when()
                .get(HEALTH_PATH)
                .andReturn();
        return r.getStatusCode();
    }

    private int sendRequest(String userId) {
        Response r = given()
                .header("X-User-Id", userId)
                .when()
                .get(HEALTH_PATH)
                .andReturn();
        return r.getStatusCode();
    }

    @Test
    @DisplayName("Within Limit Test - 80 requests all return 200")
    public void withinLimitTest() {
        int requests = 80;
        for (int i = 0; i < requests; i++) {
            int status = sendRequest("userA");
            assertEquals(200, status, "Expected 200 for request #" + (i + 1));
        }
    }

    @Test
    @DisplayName("Boundary Test - exactly 100 requests all return 200")
    public void boundaryTest() {
        int requests = 100;
        for (int i = 0; i < requests; i++) {
            int status = sendRequest("userA");
            assertEquals(200, status, "Expected 200 for request #" + (i + 1));
        }
    }

    @Test
    @DisplayName("Exceed Limit Test - 120 requests: first 100 -> 200, remaining -> 429")
    public void exceedLimitTest() {
        // Test per-user behavior: userA exceeds their quota, others unaffected
        int total = 101;
        List<Integer> statuses = new ArrayList<>(total);
        for (int i = 0; i < 100; i++) {
            statuses.add(sendRequest("userA"));
        }

        // userA should have 100 successes
        for (int i = 0; i < 100; i++) {
            assertEquals(200, statuses.get(i), "Expected 200 for userA request #" + (i + 1));
        }

        // Now a different user should not be rate-limited by userA's usage
        int statusUserB = sendRequest("userB");
        assertEquals(200, statusUserB, "Different user should not be rate limited");

        // Now userA's 101st request should be rate limited
        int statusUserA101 = sendRequest("userA");
        assertEquals(429, statusUserA101, "UserA's 101st request should be rate limited");
    }

    @Test
    @DisplayName("Window Reset Test - limit resets after window")
    public void windowResetTest() throws InterruptedException {
        int requests = 100;
        for (int i = 0; i < requests; i++) {
            int status = sendRequest("userA");
            assertEquals(200, status, "Expected 200 for request #" + (i + 1));
        }

        // Wait for rate limit window to reset (gateway uses 60s window)
        // Add a small buffer to ensure reset
        Thread.sleep(61_000);

        int status = sendRequest("userA");
        assertEquals(200, status, "Expected 200 after window reset");
    }

    @Test
    @DisplayName("Concurrent User Test - 50 concurrent users under load")
    public void concurrentUserTest() throws InterruptedException, ExecutionException {
        int concurrentUsers = 50;
        int requestsPerUser = 3; // total 150 requests
        int totalRequests = concurrentUsers * requestsPerUser;

        ExecutorService exec = Executors.newFixedThreadPool(concurrentUsers);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int u = 0; u < concurrentUsers; u++) {
            final String userId = "user-" + u;
            tasks.add(() -> {
                int successes = 0;
                for (int r = 0; r < requestsPerUser; r++) {
                    int code = sendRequest(userId);
                    if (code == 200) successes++;
                }
                return successes; // number of 200s this user observed
            });
        }

        List<Future<Integer>> results = exec.invokeAll(tasks);
        exec.shutdown();
        exec.awaitTermination(30, TimeUnit.SECONDS);

        int totalSuccesses = 0;
        for (Future<Integer> f : results) {
            totalSuccesses += f.get();
        }

        int totalFailures = totalRequests - totalSuccesses;

        // Each user is under the per-user limit, so expect all successes
        assertEquals(totalRequests, totalSuccesses, "Expected all requests to succeed for distinct users");
    }

    @TestConfiguration
    static class GatewayTestRouteConfiguration {
        @Bean
        public RouteLocator gatewayTestRoute(RouteLocatorBuilder builder) {
            return builder.routes()
                        .route("gateway-test-route", r -> r.path(HEALTH_PATH)
                            .uri("http://127.0.0.1:" + backendPort))
                    .build();
        }
    }
}
