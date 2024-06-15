package simpledb.server;

import java.io.File;
import lombok.Getter;
import lombok.experimental.Accessors;
import simpledb.buffer.BufferManager;
import simpledb.file.FileManager;
import simpledb.log.LogManager;

@Accessors(fluent = true)
@Getter
public class SimpleDB {
  public static final int BLOCK_SIZE = 400;
  public static final int BUFFER_SIZE = 8;
  public static final String LOG_FILE = "simpledb.log";

  private final FileManager fileManager;
  private final LogManager logManager;
  private final BufferManager bufferManager;

  public SimpleDB(String directoryName, int blockSize, int bufferSize) {
    File dbDirectory = new File(directoryName);
    this.fileManager = new FileManager(dbDirectory, blockSize);
    this.logManager = new LogManager(fileManager, LOG_FILE);
    this.bufferManager = new BufferManager(this.fileManager, this.logManager, bufferSize);
  }
}
