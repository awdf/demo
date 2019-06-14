package com.example.demo;

import com.example.demo.server.ActionRequest;
import com.example.demo.server.WalletGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.example.demo.server.ActionRequest.Currency.GBP;
import static com.example.demo.server.ActionRequest.Operation.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class IntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    private static ManagedChannel channel;
    private static WalletGrpc.WalletBlockingStub stub;
    private static final Pattern PATTERN = Pattern.compile("(\\S+|\\s)+");

    @BeforeAll
    static void channelUp(){
        channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                .usePlaintext()
                .build();
        stub = WalletGrpc.newBlockingStub(channel);
    }

    @ParameterizedTest
    @ValueSource(ints = {-100, 0, 100})
    public void deposit(int amount){
        ActionRequest request = ActionRequest.newBuilder()
                .setOperation(DEPOSIT)
                .setCurrency(GBP)
                .setAmount(amount)
                .setUser(1)
                .build();

        assertEquals("done", stub.action(request).getMessage());
    }


    @ParameterizedTest
    @ValueSource(ints = {-100, 0, 100})
    public void withdraw(int amount){
        ActionRequest request = ActionRequest.newBuilder()
        .setOperation(WITHDRAW)
        .setCurrency(GBP)
        .setAmount(amount)
        .setUser(1)
        .build();

        assertEquals("done", stub.action(request).getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    public void balance(int user_id){
        ActionRequest request = ActionRequest.newBuilder()
                .setOperation(BALANCE)
                .setUser(user_id)
                .build();

        assertTrue(PATTERN.matcher(stub.action(request).getMessage()).matches());
    }

    @ParameterizedTest
    @ValueSource(ints = {1000})
    public void PerformanceTest(int duration){
        ActionRequest request = ActionRequest.newBuilder().setOperation(PING).build();

        long operations = 0;
        Instant start = Instant.now();
        do {
            operations++;
            assertEquals(stub.action(request).getMessage(), "pong");
        } while (Duration.between(start, Instant.now()).toMillis() < duration);
        log.info("gRPC Performance {} (ops): ", operations);
    }

    @AfterAll
    static void channelDown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
