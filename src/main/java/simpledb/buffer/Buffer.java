package simpledb.buffer;

import lombok.Getter;
import lombok.experimental.Accessors;
import simpledb.file.BlockId;
import simpledb.file.FileManager;
import simpledb.file.Page;
import simpledb.log.LogManager;

@Accessors(fluent = true)
public class Buffer {

  private final FileManager fileManager;
  private final LogManager logManager;
  @Getter private final Page contents;

  @Getter private BlockId blockId;
  private int pins = 0;
  private int transactionNumber = -1;
  private int logSequenceNumber = -1;

  public Buffer(FileManager fileManager, LogManager logManager) {
    this.fileManager = fileManager;
    this.logManager = logManager;
    this.contents = new Page(fileManager.blockSize());
  }

  public void setModified(int transactionNumber, int logSequenceNumber) {
    this.transactionNumber = transactionNumber;
    if (logSequenceNumber >= 0) this.logSequenceNumber = logSequenceNumber;
  }

  public boolean isNotPinned() {
    return pins <= 0;
  }

  public int modifyingTransaction() {
    return transactionNumber;
  }

  /**
   * 新たなブロックをバッファに割り当てる. 割り当てる前に元々保持していたページの値を元々保持していたブロックに書き込む.
   *
   * @param blockId
   */
  void assignToBlock(BlockId blockId) {
    flush();

    this.blockId = blockId;
    fileManager.read(this.blockId, this.contents);
    pins = 0;
  }

  void flush() {
    if (transactionNumber >= 0) {
      logManager.flush(this.logSequenceNumber);
      fileManager.write(this.blockId, this.contents);
      transactionNumber = -1;
    }
  }

  void pin() {
    pins++;
  }

  void unpin() {
    pins--;
  }
}
