package dev.oswaldorosales.greeter.grpc;

import dev.oswaldorosales.grpc.greeter.FileChunk;
import dev.oswaldorosales.grpc.greeter.FileRequest;
import dev.oswaldorosales.grpc.greeter.GreeterGrpc;
import dev.oswaldorosales.grpc.greeter.HelloReply;
import dev.oswaldorosales.grpc.greeter.HelloRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Gatherers;

@GrpcService
public class GreeterGrpcService extends GreeterGrpc.GreeterImplBase {

    private static final int CHUNK_SIZE = 100;

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

    @Override
    public void downloadFile(FileRequest request, StreamObserver<FileChunk> responseObserver) {
        var filename = request.getFilename();
        var resource = getClass().getClassLoader().getResource("data/" + filename);

        if (resource == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("File not found: " + filename)
                    .asRuntimeException());
            return;
        }

        try (var reader = Files.newBufferedReader(Path.of(resource.toURI()))) {
            var chunkNumber = new AtomicInteger(0);

            reader.lines()
                    .gather(Gatherers.windowFixed(CHUNK_SIZE))
                    .forEach(lines -> responseObserver.onNext(
                            FileChunk.newBuilder()
                                    .addAllLines(lines)
                                    .setChunkNumber(chunkNumber.incrementAndGet())
                                    .build()
                    ));

            responseObserver.onCompleted();

        } catch (IOException | URISyntaxException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error reading file: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
