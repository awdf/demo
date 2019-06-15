package com.example.demo;

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

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    //Client planned actions counter
    private static long plannedActions = users; //Create user action is planned

    //Manage gRPC channel
    private final ManagedChannel channel;
    private final WalletGrpc.WalletBlockingStub bstub;
    private final WalletGrpc.WalletFutureStub fstub;


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
        // Channels are secure by default (via SSL/TLS).
        // For the best results we disable TLS to avoid security delays.
        this.channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                .usePlaintext()
                .build();
        this.bstub = WalletGrpc.newBlockingStub(channel);
        this.fstub = WalletGrpc.newFutureStub(channel);
    }

    private void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void walletClientRun() throws InterruptedException {
        /* Client execution plan:
        1. Create users
        2. Emulate users work
        3. Finish client
        */
        List<Integer> users = createUsers();
        this.doWork(users);
        this.shutdown();

        log.info("User planned actions {}", plannedActions);
        System.exit(0);
    }

    private List<Integer> createUsers(){
        //List with user IDs that reserve pool thread.
        // One ID per thread.
        List<Integer> usersId = new LinkedList<>();

        //According to Client requirements will do both
        // create users [0..users] and
        // reserve [0..concurrentThreadsPerUser] pool threads for each user.
        IntStream.range(0, users)
        .mapToObj(n -> bstub.action(ActionRequest.newBuilder().setOperation(CREATE).build()))
        .mapToInt(reply -> Integer.parseInt(reply.getMessage()))
        .forEach(id -> {
            IntStream.range(0, concurrentThreadsPerUser).mapToObj(j -> id).forEach(usersId::add);
        });

        return usersId;
    }

    private void doWork(List<Integer> usersId) {
        Instant start = Instant.now();

        //Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(users * concurrentThreadsPerUser);

        //Starts user work emulation with concurrency
        List<CompletableFuture<Boolean>> futures = usersId.stream()
                .map(f -> CompletableFuture.completedFuture(f)
                        .thenApplyAsync(this::userEmulation, executor)
                )
                .collect(Collectors.toList());

        //Checks that all have been well
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenRun(() -> futures.forEach((cf) -> {assert cf.getNow(false);}))
                .join();

        Instant finish = Instant.now();

        log.info("Threads execution time " + Duration.between(start, finish).toMillis());

        executor.shutdown();
    }

    private boolean userEmulation(Integer user_id){
        if (user_id == null) return false;

        //Get user rounds randomly
        List<ActionRequest> actions = new LinkedList<>();
        for (int i = 0; i < roundsPerThread; i++) {
            actions.addAll(getRoundCollection(user_id, Round.getAnyOne()));
        }
        log.info("User {} action list length {}", user_id, actions.size());
        plannedActions += actions.size();

        //Emulate user next

        //Blocking stub usage too slow
//        actions.stream().map((action)->bstub.action(action).getMessage())
//                .collect(Collectors.toList())
//                .forEach(log::debug);

        //Future stub is much faster
        actions.stream().forEach(fstub::action);

        return true;
    }

    private List<ActionRequest> getRoundCollection(int user, Round round) {
        List<ActionRequest> collection = new LinkedList<>();

        //Prepared user action rounds
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
        //For each user will send CREATE action
        WalletClient.plannedActions = users;
    }

    static void setConcurrentThreadsPerUser(int concurrentThreadsPerUser) {
        WalletClient.concurrentThreadsPerUser = concurrentThreadsPerUser;
    }

    static void setRoundsPerThread(int roundsPerThread) {
        WalletClient.roundsPerThread = roundsPerThread;
    }
}