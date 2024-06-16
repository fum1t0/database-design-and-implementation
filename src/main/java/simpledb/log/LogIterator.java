package simpledb.log;

import java.util.Iterator;
import simpledb.file.BlockId;
import simpledb.file.FileManager;
import simpledb.file.Page;

class LogIterator implements Iterator<byte[]> {

  private final FileManager fileManager;
  private final Page page;

  private BlockId blockId;
  private int currentPosition = 0;

  public LogIterator(FileManager fileManager, BlockId blockId) {
    this.fileManager = fileManager;
    this.blockId = blockId;
    this.page = new Page(new byte[fileManager.blockSize()]);
    moveToBlock(blockId);
  }

  @Override
  public boolean hasNext() {
    return currentPosition < fileManager.blockSize() || blockId.blockNumber() > 0;
  }

  @Override
  public byte[] next() {
    if (currentPosition == fileManager.blockSize()) {
      blockId = new BlockId(blockId.fileName(), blockId.blockNumber() - 1);
      moveToBlock(blockId);
    }

    byte[] record = page.getBytes(currentPosition);
    currentPosition += page.neededBytes(record);
    return record;
  }

  private void moveToBlock(BlockId blockId) {
    fileManager.read(blockId, page);
    currentPosition = page.getInt(0);
  }
}
