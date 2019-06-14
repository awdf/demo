package com.example.demo.services.Impl;

import com.example.demo.models.User;
import com.example.demo.server.ActionReply;
import com.example.demo.server.ActionRequest;
import com.example.demo.server.WalletGrpc;
import com.example.demo.services.AccountManagmentService;
import com.example.demo.services.BaseService;
import com.example.demo.services.CurrencyManagmentService;
import com.example.demo.services.UserManagmentService;
import org.lognet.springboot.grpc.GRpcService;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;

@GRpcService
public class GrpcServerServiceImpl extends WalletGrpc.WalletImplBase implements BaseService {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerServiceImpl.class);
    private static long counter = 0;

    private final UserManagmentService userService;

    private final AccountManagmentService accountService;

    private final CurrencyManagmentService currencyService;

    public GrpcServerServiceImpl(UserManagmentService userService, AccountManagmentService accountService, CurrencyManagmentService currencyService) {
        this.userService = userService;
        this.accountService = accountService;
        this.currencyService = currencyService;
    }

    @Override
    public void action(ActionRequest request, StreamObserver<ActionReply> responseObserver) {
        log.debug("Request {} received {} ", ++counter, request.getOperation().name());

        String message = "done";
        try {
            switch (request.getOperation()){
                case PING:
                    message = "pong";
                    break;
                case BALANCE:
                    message = accountService.balance(request.getUser());
                    break;
                case DEPOSIT:
                    accountService.deposit(request.getUser(), request.getAmount(), request.getCurrency().name());
                    break;
                case WITHDRAW:
                    accountService.withdraw(request.getUser(), request.getAmount(), request.getCurrency().name());
                    break;
                case CREATE:
                    User user = userService.createUser(currencyService.getAllCurrencies());
                    message = String.valueOf(user.getId());
                    break;
                case UNRECOGNIZED:
                    message = request.getOperation().name();
                    break;
            }
        } catch (Exception ex) {
            message = ex.getMessage();
        }

        ActionReply reply = ActionReply.newBuilder().setMessage(message).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @PreDestroy
    public void onDestroy(){
        log.info("gRPC service total {}", counter);
    }

    @Override
    public void initService() {}
}
