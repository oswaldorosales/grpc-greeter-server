package dev.oswaldorosales.greeter.grpc;

import dev.oswaldorosales.grpc.greeter.FileChunk;
import dev.oswaldorosales.grpc.greeter.FileRequest;
import dev.oswaldorosales.grpc.greeter.HelloReply;
import dev.oswaldorosales.grpc.greeter.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GreeterGrpcServiceTest {

    @InjectMocks
    private GreeterGrpcService greeterGrpcService;

    @Mock
    private StreamObserver<HelloReply> helloObserver;

    @Mock
    private StreamObserver<FileChunk> fileObserver;

    @Test
    void sayHello_shouldReturnGreetingWithGivenName() {
        //given
        HelloRequest request = HelloRequest.newBuilder()
                .setName("Oswaldo")
                .build();

        //when
        greeterGrpcService.sayHello(request, helloObserver);

        //then
        ArgumentCaptor<HelloReply> replyCaptor = ArgumentCaptor.forClass(HelloReply.class);
        verify(helloObserver).onNext(replyCaptor.capture());
        assertThat(replyCaptor.getValue().getMessage(), is(equalTo("Hello, Oswaldo!")));
    }

    @Test
    void sayHello_shouldCompleteTheObserver() {
        //given
        HelloRequest request = HelloRequest.newBuilder()
                .setName("Oswaldo")
                .build();

        //when
        greeterGrpcService.sayHello(request, helloObserver);

        //then
        verify(helloObserver).onCompleted();
    }

    @Test
    void downloadFile_shouldStreamFileInChunks() {
        //given
        FileRequest request = FileRequest.newBuilder()
                .setFilename("test.csv")
                .build();

        //when
        greeterGrpcService.downloadFile(request, fileObserver);

        //then
        // test.csv has 6 lines (header + 5 rows), chunk size 100 → 1 chunk
        ArgumentCaptor<FileChunk> chunkCaptor = ArgumentCaptor.forClass(FileChunk.class);
        verify(fileObserver).onNext(chunkCaptor.capture());
        verify(fileObserver).onCompleted();

        List<FileChunk> chunks = chunkCaptor.getAllValues();
        assertThat(chunks, hasSize(1));
        assertThat(chunks.getFirst().getChunkNumber(), is(1));
        assertThat(chunks.getFirst().getLinesList(), hasSize(6));
        assertThat(chunks.getFirst().getLines(0), containsString("id,name,email"));
    }

    @Test
    void downloadFile_shouldReturnNotFoundForMissingFile() {
        //given
        FileRequest request = FileRequest.newBuilder()
                .setFilename("nonexistent.csv")
                .build();

        //when
        greeterGrpcService.downloadFile(request, fileObserver);

        //then
        verify(fileObserver).onError(org.mockito.ArgumentMatchers.any());
        verify(fileObserver, times(0)).onCompleted();
    }
}
