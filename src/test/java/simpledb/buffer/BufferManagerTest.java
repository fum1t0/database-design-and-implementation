package simpledb.buffer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simpledb.file.BlockId;
import simpledb.server.SimpleDB;

public class BufferManagerTest {

  private static final String DB_FILENAME = "dbtest";

  BufferManager bufferManager;

  @BeforeEach
  void setup() {
    bufferManager = new SimpleDB(DB_FILENAME, SimpleDB.BLOCK_SIZE, 3).bufferManager();
  }

  @Test
  void throwExceptionIfNoBuffersAreAvailable() {
    // setup
    Buffer[] buffers = new Buffer[6];
    IntStream.range(0, 3).forEach(i -> buffers[i] = bufferManager.pin(new BlockId(DB_FILENAME, i)));

    // invoke and verify
    assertThrows(
        BufferAbortException.class,
        () -> buffers[5] = bufferManager.pin(new BlockId(DB_FILENAME, 3)));
  }

  @Test
  void canPinBufferIfAnyBuffersAreAvailable() {
    // setup
    Buffer[] buffers = new Buffer[6];
    IntStream.range(0, 3).forEach(i -> buffers[i] = bufferManager.pin(new BlockId(DB_FILENAME, i)));

    assertDoesNotThrow(
        () -> {
          bufferManager.unpin(buffers[1]);
          buffers[1] = null;

          buffers[3] = bufferManager.pin(new BlockId(DB_FILENAME, 0));
          buffers[4] = bufferManager.pin(new BlockId(DB_FILENAME, 1));

          bufferManager.unpin(buffers[2]);
          buffers[2] = null;
          buffers[5] = bufferManager.pin(new BlockId(DB_FILENAME, 3));
        });
  }
}
