package com.example.demo;

import com.example.demo.server.ActionReply;
import com.example.demo.server.ActionRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import com.example.demo.server.WalletGrpc;

import java.util.concurrent.TimeUnit;

import static com.example.demo.server.ActionRequest.Operation.*;
import static com.example.demo.server.ActionRequest.Currency.*;

@Component
@Profile("work")
public class WalletClient {
    private static final Logger log = LoggerFactory.getLogger(WalletClient.class);

    private final ManagedChannel channel;
    private final WalletGrpc.WalletBlockingStub stub;

    public WalletClient() {
        // we should get this info from a service like eureka
        this.channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                // Channels are secure by default (via SSL/TLS).
                // For the example we disable TLS to avoid needing certificates.
                .usePlaintext()
                .build();
        this.stub = WalletGrpc.newBlockingStub(channel);

    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startup() throws InterruptedException {
        this.doAction();
        this.shutdown();
        System.exit(0);
    }

    public void doAction() throws InterruptedException {
//        ActionRequest request = ActionRequest.newBuilder()
//                .setOperation(BALANCE)
//                .setUser(1)
//                .build();

//        ActionRequest request = ActionRequest.newBuilder()
//                .setOperation(WITHDRAW)
//                .setCurrency(GBP)
//                .setAmount(100)
//                .setUser(1)
//                .build();

        ActionRequest request = ActionRequest.newBuilder()
                .setOperation(DEPOSIT)
                .setCurrency(GBP)
                .setAmount(100)
                .setUser(20)
                .build();

        while(true){
            try {
                ActionReply response = stub.action(request);
                log.info("Response: " + response.getMessage());
                return;
            } catch (StatusRuntimeException e) {
                log.info("ERROR ON: " + e.getMessage());
                Thread.sleep(1000);
            }
        }
    }



}