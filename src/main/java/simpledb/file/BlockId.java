package simpledb.file;

public record BlockId(String filename, int blockNumber) {

  public boolean equals(Object obj) {
    BlockId other = (BlockId) obj;
    return this.filename.equals(other.filename) && this.blockNumber == other.blockNumber;
  }

  public String toString() {
    return String.format("[file %s, block %d]", this.filename, this.blockNumber);
  }

  public int hashCode() {
    return toString().hashCode();
  }
}
