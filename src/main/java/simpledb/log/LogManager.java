package simpledb.log;

import java.util.Iterator;
import simpledb.file.BlockId;
import simpledb.file.FileManager;
import simpledb.file.Page;

public class LogManager {

  private final FileManager fileManager;
  private final String logFile;
  private final Page logPage;

  private BlockId currentBlockId;
  private int latestLogSequenceNumber = 0;
  private int lastSavedLogSequenceNumber = 0;

  public LogManager(FileManager fileManager, String logFile) {
    this.fileManager = fileManager;
    this.logFile = logFile;
    this.logPage = new Page(new byte[fileManager.blockSize()]);

    int logSize = fileManager.length(logFile);
    if (logSize == 0) {
      this.currentBlockId = appendNewBlock();
    } else {
      // 既にログが存在する場合は最後のブロックの値を page に読み込む
      this.currentBlockId = new BlockId(logFile, logSize - 1);
      fileManager.read(this.currentBlockId, this.logPage);
    }
  }

  public void flush(int logSequenceNumber) {
    if (logSequenceNumber >= lastSavedLogSequenceNumber) flush();
  }

  public Iterator<byte[]> iterator() {
    flush();
    return new LogIterator(fileManager, currentBlockId);
  }

  /**
   * ログを書き込む. 書き込むログに対して空き容量が不足していた場合は page の内容をログファイルに書き込み、新たな block を作成する. 書き込んだ後は page
   * の先頭の書き込み可能サイズを更新する.
   *
   * @param logRecord
   * @return 書き込まれた LogSequenceNumber
   */
  public synchronized int append(byte[] logRecord) {
    int boundary = logPage.getInt(0);
    int neededBytes = logPage.neededBytes(logRecord);
    if (boundary - neededBytes < Integer.BYTES) {
      flush();
      currentBlockId = appendNewBlock();
      boundary = logPage.getInt(0);
    }

    int recordPosition = boundary - neededBytes;
    logPage.setBytes(recordPosition, logRecord);
    logPage.setInt(0, recordPosition);
    return ++latestLogSequenceNumber;
  }

  /**
   * 新しい block を追加する. Page の先頭には書込み可能なサイズとして FileManager が管理するブロックサイズを記録する.
   *
   * @return blockId
   */
  private BlockId appendNewBlock() {
    BlockId blockId = fileManager.append(logFile);
    logPage.setInt(0, fileManager.blockSize());
    fileManager.write(blockId, logPage);
    return blockId;
  }

  private void flush() {
    fileManager.write(currentBlockId, logPage);
    lastSavedLogSequenceNumber = latestLogSequenceNumber;
  }
}
