package simpledb.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class FileManager {
  private final File dbDirectory;
  @Getter private final int blockSize;
  @Getter private final boolean isNew;
  private final Map<String, RandomAccessFile> openFiles = new HashMap<>();

  public FileManager(File dbDirectory, int blockSize) {
    this.dbDirectory = dbDirectory;
    this.blockSize = blockSize;
    isNew = !dbDirectory.exists();

    // create the directory if the database is new
    if (isNew) {
      dbDirectory.mkdirs();
    }

    // remove any leftover temporary tables
    for (String filename : dbDirectory.list()) {
      if (filename.startsWith("temp")) {
        new File(dbDirectory, filename).delete();
      }
    }
  }

  public synchronized void read(BlockId blockId, Page page) {
    try {
      RandomAccessFile file = getFile(blockId.filename());
      file.seek(blockId.blockNumber() * blockSize);
      file.getChannel().read(page.contents());
    } catch (IOException _) {
      throw new RuntimeException(STR."cannot read block %s\{blockId}");
    }
  }

  public synchronized void write(BlockId blockId, Page page) {
    try {
      RandomAccessFile file = getFile(blockId.filename());
      file.seek(blockId.blockNumber() * blockSize);
      file.getChannel().write(page.contents());
    } catch (IOException _) {
      throw new RuntimeException(STR."cannot write block \{blockId}");
    }
  }

  public synchronized void append(String filename) {
    int newBlockNumber = length(filename);
    BlockId blockId = new BlockId(filename, newBlockNumber);
    byte[] bytes = new byte[blockSize];
    try {
      RandomAccessFile file = getFile(blockId.filename());
      file.seek(blockId.blockNumber() * blockSize);
      file.write(bytes);
    } catch (IOException _) {
      throw new RuntimeException(STR."cannot append block \{blockId}");
    }
  }

  public int length(String filename) {
    try {
      RandomAccessFile file = getFile(filename);
      return (int) (file.length() / blockSize);
    } catch (IOException _) {
      throw new RuntimeException(STR."cannot access \{filename}");
    }
  }

  private RandomAccessFile getFile(String filename) throws IOException {
    RandomAccessFile file = openFiles.get(filename);
    if (Objects.isNull(file)) {
      File dbTable = new File(dbDirectory, filename);
      file = new RandomAccessFile(dbTable, "rws");
      openFiles.put(filename, file);
    }
    return file;
  }
}
