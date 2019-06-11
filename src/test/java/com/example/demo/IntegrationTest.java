package com.example.demo;

import com.example.demo.server.ActionRequest;
import com.example.demo.server.WalletGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.example.demo.server.ActionRequest.Operation.*;
import static com.example.demo.server.ActionRequest.Currency.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class IntegrationTest {

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

    @AfterAll
    static void channelDown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
