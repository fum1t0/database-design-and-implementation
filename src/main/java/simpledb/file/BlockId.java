package simpledb.file;

public record BlockId(String fileName, int blockNumber) {

  public boolean equals(Object obj) {
    BlockId other = (BlockId) obj;
    return this.fileName.equals(other.fileName) && this.blockNumber == other.blockNumber;
  }

  public String toString() {
    return String.format("[file %s, block %d]", this.fileName, this.blockNumber);
  }

  public int hashCode() {
    return toString().hashCode();
  }
}
