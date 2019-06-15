package com.example.demo;

import com.example.demo.server.ActionReply;
import com.example.demo.server.ActionRequest;
import com.example.demo.server.WalletGrpc;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
    private static WalletGrpc.WalletBlockingStub bstub;
    private static WalletGrpc.WalletFutureStub fstub;
    private static final Pattern PATTERN = Pattern.compile("(\\S+|\\s)+");
    private int user_id = -1;

    @BeforeAll
    static void channelUp(){
        channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                .usePlaintext()
                .build();
        bstub = WalletGrpc.newBlockingStub(channel);
        fstub = WalletGrpc.newFutureStub(channel);
    }

    @BeforeEach
    public void createUser(){
        if (user_id < 0) {
            ActionRequest request = ActionRequest.newBuilder()
                    .setOperation(CREATE)
                    .build();

            user_id = Integer.parseInt(bstub.action(request).getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-100, 0, 100})
    public void deposit(int amount){
        ActionRequest request = ActionRequest.newBuilder()
                .setOperation(DEPOSIT)
                .setCurrency(GBP)
                .setAmount(amount)
                .setUser(user_id)
                .build();

        assertEquals("done", bstub.action(request).getMessage());
    }


    @ParameterizedTest
    @ValueSource(ints = {-100, 0, 100})
    public void withdraw(int amount){
        ActionRequest request = ActionRequest.newBuilder()
        .setOperation(WITHDRAW)
        .setCurrency(GBP)
        .setAmount(amount)
        .setUser(user_id)
        .build();

        assertTrue(PATTERN.matcher(bstub.action(request).getMessage()).matches());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    public void balance(int user_id){
        ActionRequest request = ActionRequest.newBuilder()
                .setOperation(BALANCE)
                .setUser(user_id)
                .build();

        assertTrue(PATTERN.matcher(bstub.action(request).getMessage()).matches());
    }

    @ParameterizedTest
    @ValueSource(ints = {1000})
    public void PerformanceTest(int duration){
        ActionRequest request = ActionRequest.newBuilder().setOperation(PING).build();

        long operations = 0;
        Instant start = Instant.now();
        do {
            operations++;
            ListenableFuture<ActionReply> reply = fstub.action(request);
            Futures.addCallback(reply, new Callback(), MoreExecutors.directExecutor());
        } while (Duration.between(start, Instant.now()).toMillis() < duration);
        log.info("gRPC performance {} (ops) per {} ms ", operations, Duration.between(start, Instant.now()).toMillis() );
    }

    @AfterAll
    static void channelDown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}

class Callback implements FutureCallback<ActionReply> {
    private static final Logger log = LoggerFactory.getLogger(Callback.class);

    @Override
    public void onSuccess(ActionReply result) {
        log.debug(result.getMessage());
    }

    @Override
    public void onFailure(Throwable t) {
        log.error("Concurrency issue", t);
    }
}