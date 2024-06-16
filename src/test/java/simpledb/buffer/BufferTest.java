package simpledb.buffer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simpledb.file.BlockId;
import simpledb.file.Page;
import simpledb.server.SimpleDB;

public class BufferTest {

  BufferManager bufferManager;

  @BeforeEach
  void setup() {
    String dbName = this.getClass().getSimpleName();
    bufferManager = new SimpleDB(dbName, SimpleDB.BLOCK_SIZE, 3).bufferManager();
  }

  @Test
  void canFlushWhenOtherBuffersArePinned() {
    // setup
    String fileName = "canFlushWhenOtherBuffersArePinned";
    Buffer buffer = bufferManager.pin(new BlockId(fileName, 1));
    Page page = buffer.contents();
    int offset = 80;
    int n = page.getInt(offset);

    page.setInt(offset, n + 1);
    buffer.setModified(1, 0);
    buffer.unpin();

    // invoke and verify
    assertDoesNotThrow(
        () -> IntStream.range(2, 5).forEach(i -> bufferManager.pin(new BlockId(fileName, i))));
  }
}
