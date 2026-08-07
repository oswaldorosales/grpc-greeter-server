package dev.oswaldorosales.greeter.grpc;

import dev.oswaldorosales.grpc.greeter.GreeterGrpc;
import dev.oswaldorosales.grpc.greeter.HelloReply;
import dev.oswaldorosales.grpc.greeter.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class GreeterGrpcService extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        String message = "Hello, " + name + "!";

        HelloReply reply = HelloReply.newBuilder()
                .setMessage(message)
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
