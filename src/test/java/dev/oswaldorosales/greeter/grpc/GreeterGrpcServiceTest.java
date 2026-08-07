package dev.oswaldorosales.greeter.grpc;

import dev.oswaldorosales.grpc.greeter.HelloReply;
import dev.oswaldorosales.grpc.greeter.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GreeterGrpcServiceTest {

    @InjectMocks
    private GreeterGrpcService greeterGrpcService;

    @Mock
    private StreamObserver<HelloReply> responseObserver;

    @Test
    void sayHello_shouldReturnGreetingWithGivenName() {
        //given
        HelloRequest request = HelloRequest.newBuilder()
                .setName("Oswaldo")
                .build();

        //when
        greeterGrpcService.sayHello(request, responseObserver);

        //then
        ArgumentCaptor<HelloReply> replyCaptor = ArgumentCaptor.forClass(HelloReply.class);
        verify(responseObserver).onNext(replyCaptor.capture());
        assertThat(replyCaptor.getValue().getMessage(), is(equalTo("Hello, Oswaldo!")));
    }

    @Test
    void sayHello_shouldCompleteTheObserver() {
        //given
        HelloRequest request = HelloRequest.newBuilder()
                .setName("Oswaldo")
                .build();

        //when
        greeterGrpcService.sayHello(request, responseObserver);

        //then
        verify(responseObserver).onCompleted();
    }
}
