package dev.oswaldorosales.greeter.integration;

import dev.oswaldorosales.grpc.greeter.GreeterGrpc;
import dev.oswaldorosales.grpc.greeter.HelloReply;
import dev.oswaldorosales.grpc.greeter.HelloRequest;
import io.grpc.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureTestGrpcTransport
class GreeterGrpcServiceIT {

    @Autowired
    private GrpcChannelFactory channelFactory;

    @Test
    void sayHello_shouldReturnGreetingMessage() {
        //given
        Channel channel = channelFactory.createChannel("test");
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        HelloRequest request = HelloRequest.newBuilder()
                .setName("Oswaldo")
                .build();

        //when
        HelloReply reply = stub.sayHello(request);

        //then
        assertThat(reply.getMessage(), is(equalTo("Hello, Oswaldo!")));
    }
}
