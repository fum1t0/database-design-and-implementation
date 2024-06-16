package simpledb.buffer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simpledb.file.BlockId;
import simpledb.server.SimpleDB;

public class BufferManagerTest {

  BufferManager bufferManager;

  @BeforeEach
  void setup() {
    String dbName = this.getClass().getSimpleName();
    bufferManager = new SimpleDB(dbName, SimpleDB.BLOCK_SIZE, 3).bufferManager();
  }

  @Test
  void throwExceptionIfNoBuffersAreAvailable() {
    // setup
    String fileName = "throwExceptionIfNoBuffersAreAvailable";
    Buffer[] buffers = new Buffer[6];
    IntStream.range(0, 3).forEach(i -> buffers[i] = bufferManager.pin(new BlockId(fileName, i)));

    // invoke and verify
    assertThrows(
        BufferAbortException.class, () -> buffers[5] = bufferManager.pin(new BlockId(fileName, 3)));
  }

  @Test
  void canPinBufferIfAnyBuffersAreAvailable() {
    // setup
    String fileName = "canPinBufferIfAnyBuffersAreAvailable";
    Buffer[] buffers = new Buffer[6];
    IntStream.range(0, 3).forEach(i -> buffers[i] = bufferManager.pin(new BlockId(fileName, i)));

    assertDoesNotThrow(
        () -> {
          bufferManager.unpin(buffers[1]);
          buffers[1] = null;

          buffers[3] = bufferManager.pin(new BlockId(fileName, 0));
          buffers[4] = bufferManager.pin(new BlockId(fileName, 1));

          bufferManager.unpin(buffers[2]);
          buffers[2] = null;
          buffers[5] = bufferManager.pin(new BlockId(fileName, 3));
        });
  }
}
