package com.example.demo.services.Impl;

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
import org.springframework.beans.factory.annotation.Autowired;

@GRpcService
public class GrpcServerServiceImpl extends WalletGrpc.WalletImplBase implements BaseService {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerServiceImpl.class);

    @Autowired
    UserManagmentService userService;

    @Autowired
    AccountManagmentService accountService;

    @Autowired
    CurrencyManagmentService currencyService;

    @Override
    public void action(ActionRequest request, StreamObserver<ActionReply> responseObserver) {
        log.info("Request received!");
        String message = "done";
        try {
            switch (request.getOperation()){
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
                    userService.createUser(currencyService.getAllCurrencies());
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

    @Override
    public void initService() {

    }
}
