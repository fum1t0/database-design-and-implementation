package simpledb.server;

import java.io.File;
import lombok.Getter;
import lombok.experimental.Accessors;
import simpledb.file.FileManager;

@Accessors(fluent = true)
@Getter
public class SimpleDB {
  public static final int BLOCK_SIZE = 400;
  public static final int BUFFER_SIZE = 8;
  public static final String LOG_FILE = "simpledb.log";

  private FileManager fileManager;

  public SimpleDB(String directoryName, int blockSize, int bufferSize) {
    File dbDirectory = new File(directoryName);
    fileManager = new FileManager(dbDirectory, blockSize);
  }
}
