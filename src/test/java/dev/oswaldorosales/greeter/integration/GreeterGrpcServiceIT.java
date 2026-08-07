package dev.oswaldorosales.greeter.integration;

import dev.oswaldorosales.grpc.greeter.FileChunk;
import dev.oswaldorosales.grpc.greeter.FileRequest;
import dev.oswaldorosales.grpc.greeter.GreeterGrpc;
import dev.oswaldorosales.grpc.greeter.HelloReply;
import dev.oswaldorosales.grpc.greeter.HelloRequest;
import io.grpc.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
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

    @Test
    void downloadFile_shouldStreamAllChunks() {
        //given
        Channel channel = channelFactory.createChannel("test");
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        FileRequest request = FileRequest.newBuilder()
                .setFilename("users.csv")
                .build();

        //when
        Iterator<FileChunk> stream = stub.downloadFile(request);
        List<FileChunk> chunks = new ArrayList<>();
        stream.forEachRemaining(chunks::add);

        //then
        // 2001 lines (header + 2000 rows), chunk size 100 → 21 chunks
        assertThat(chunks, org.hamcrest.Matchers.hasSize(21));
        assertThat(chunks.getFirst().getChunkNumber(), is(1));
        assertThat(chunks.getLast().getChunkNumber(), is(21));
        assertThat(chunks.getFirst().getLines(0),
                is(equalTo("id,name,email,country,amount,date")));
        assertThat(chunks.getLast().getLinesList().size(), is(greaterThan(0)));
    }
}
