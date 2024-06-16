package simpledb.buffer;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import simpledb.file.BlockId;
import simpledb.file.FileManager;
import simpledb.log.LogManager;

public class BufferManager {

  private static final long MAX_TIME_MILLISECONDS = 10000; // 10 seconds

  private final Buffer[] bufferPool;

  private int numberOfAvailable;

  public BufferManager(FileManager fileManager, LogManager logManager, int buffersNumber) {
    this.bufferPool = new Buffer[buffersNumber];
    this.numberOfAvailable = buffersNumber;
    IntStream.range(0, buffersNumber)
        .forEach(i -> this.bufferPool[i] = new Buffer(fileManager, logManager));
  }

  public synchronized int available() {
    return numberOfAvailable;
  }

  public synchronized void flushAll(int transactionNumber) {
    for (Buffer buffer : bufferPool) {
      if (buffer.modifyingTransaction() == transactionNumber) buffer.flush();
    }
  }

  public synchronized void unpin(Buffer buffer) {
    buffer.unpin();
    if (buffer.isNotPinned()) {
      numberOfAvailable++;
      notifyAll();
    }
  }

  public synchronized Buffer pin(BlockId blockId) {
    try {
      long timestamp = currentTimeMilliseconds();

      Optional<Buffer> bufferOptional = tryToPin(blockId);
      while (bufferOptional.isEmpty() && !waitingTooLong(timestamp)) {
        wait(MAX_TIME_MILLISECONDS);
        bufferOptional = tryToPin(blockId);
      }
      if (bufferOptional.isEmpty()) throw new BufferAbortException();

      return bufferOptional.get();
    } catch (InterruptedException _) {
      throw new BufferAbortException();
    }
  }

  private long currentTimeMilliseconds() {
    return System.nanoTime() * 1000 * 1000;
  }

  private boolean waitingTooLong(long startTime) {
    return currentTimeMilliseconds() - startTime > MAX_TIME_MILLISECONDS;
  }

  private Optional<Buffer> tryToPin(BlockId blockId) {
    // findExistingBuffer が空の Optional を返したときに chooseUnpinnedBuffer
    // を実行したほうが効率的だが、理解を容易にするために先に実行する形で実装.
    Optional<Buffer> existingBufferOptional = findExistingBuffer(blockId);
    Optional<Buffer> unpinnedBufferOptional = chooseUnpinnedBuffer();
    if (existingBufferOptional.isEmpty() && unpinnedBufferOptional.isEmpty())
      return Optional.empty();

    Buffer buffer = existingBufferOptional.orElseGet(unpinnedBufferOptional::get);
    buffer.assignToBlock(blockId);

    if (buffer.isNotPinned()) numberOfAvailable--;
    buffer.pin();
    return Optional.of(buffer);
  }

  private Optional<Buffer> findExistingBuffer(BlockId targetBlockId) {
    return Arrays.stream(bufferPool)
        .filter(
            buffer -> Objects.nonNull(buffer.blockId()) && buffer.blockId().equals(targetBlockId))
        .findFirst();
  }

  private Optional<Buffer> chooseUnpinnedBuffer() {
    return Arrays.stream(bufferPool).filter(Buffer::isNotPinned).findFirst();
  }
}
