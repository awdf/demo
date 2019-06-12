package com.example.demo;

import com.example.demo.server.ActionReply;
import com.example.demo.server.ActionRequest;
import com.example.demo.server.WalletGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.example.demo.server.ActionRequest.Currency.*;
import static com.example.demo.server.ActionRequest.Operation.*;

@Component
@Profile("work")
public class WalletClient {
    private static final Logger log = LoggerFactory.getLogger(WalletClient.class);

    //number of concurrent users emulated
    private static int users = 1;

    //number of concurrent requests a user will make
    private static int concurrentThreadsPerUser = 1;

    //number of rounds each thread is executing
    private static int roundsPerThread = 1;

    //Manage gRPC channel
    private final ManagedChannel channel;
    private final WalletGrpc.WalletBlockingStub stub;

    enum Round {
        A(1),B(2),C(3);

        int code;
        Round(int r) {
            code = r;
        }

        public int getCode(){
            return code;
        }

        public static Round getAnyOne(){
            int any = (int) Math.round(Round.values().length * Math.random());
            return Round.values()[any == 3 ? 0 : any];
        }
    }

    public WalletClient() {
        // we should get this info from a service like eureka
        this.channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                // Channels are secure by default (via SSL/TLS).
                // For the example we disable TLS to avoid needing certificates.
                .usePlaintext()
                .build();
        this.stub = WalletGrpc.newBlockingStub(channel);

    }

    private void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startup() throws InterruptedException {
        List<Integer> users = createUsers();
        this.doWork(users);
        this.shutdown();
        System.exit(0);
    }

    private List<Integer> createUsers(){
        List<Integer> usersId = new LinkedList<>();

        for (int i=0; i < users; i++) {
            ActionReply reply = stub.action(ActionRequest.newBuilder().setOperation(CREATE).build());
            int id = Integer.parseInt(reply.getMessage());
            for (int j = 0; j < concurrentThreadsPerUser; j++) {
                usersId.add(id);
            }
        }

        return usersId;
    }

    private void doWork(List<Integer> usersId) {

        ExecutorService executor = Executors.newFixedThreadPool(users * concurrentThreadsPerUser);
        List<CompletableFuture<Boolean>> futures = usersId.stream()
                .map(f -> CompletableFuture.completedFuture(f)
                        .thenApplyAsync(this::userEmulation, executor)
                )
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenRun(() -> futures.forEach((cf) -> {assert cf.getNow(false);}))
                .join();

        executor.shutdown();
    }

    private boolean userEmulation(Integer user_id){
        List<ActionRequest> actions = new LinkedList<>();

        for (int i = 0; i < roundsPerThread; i++) {
            actions.addAll(getRoundCollection(user_id, Round.getAnyOne()));
        }
        log.info("User {} action list length {}", user_id, actions.size());

        actions.stream().map((action)->stub.action(action).getMessage())
                .collect(Collectors.toList());
//                .forEach(log::info);

        return true;
    }

    private List<ActionRequest> getRoundCollection(int user, Round round) {
        List<ActionRequest> collection = new LinkedList<>();

        switch (round){
            case A:
                collection.add(ActionRequest.newBuilder().setOperation(DEPOSIT).setAmount(100).setCurrency(USD).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(200).setCurrency(USD).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(DEPOSIT).setAmount(100).setCurrency(EUR).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(BALANCE).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(100).setCurrency(USD).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(BALANCE).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(100).setCurrency(USD).setUser(user).build());
                break;
            case B:
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(100).setCurrency(GBP).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(DEPOSIT).setAmount(300).setCurrency(GBP).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(100).setCurrency(GBP).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(100).setCurrency(GBP).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(100).setCurrency(GBP).setUser(user).build());
                break;
            case C:
                collection.add(ActionRequest.newBuilder().setOperation(BALANCE).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(DEPOSIT).setAmount(100).setCurrency(USD).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(DEPOSIT).setAmount(100).setCurrency(USD).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(100).setCurrency(USD).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(DEPOSIT).setAmount(100).setCurrency(USD).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(BALANCE).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(WITHDRAW).setAmount(200).setCurrency(USD).setUser(user).build());
                collection.add(ActionRequest.newBuilder().setOperation(BALANCE).setUser(user).build());
                break;
        }
        return collection;
    }

    public static void setUsers(int users) {
        WalletClient.users = users;
    }

    static void setConcurrentThreadsPerUser(int concurrentThreadsPerUser) {
        WalletClient.concurrentThreadsPerUser = concurrentThreadsPerUser;
    }

    static void setRoundsPerThread(int roundsPerThread) {
        WalletClient.roundsPerThread = roundsPerThread;
    }
}