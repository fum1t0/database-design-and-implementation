package simpledb.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simpledb.server.SimpleDB;

public class FileTest {

  FileManager fileManager;

  @BeforeEach
  void setup() {
    String dbName = this.getClass().getSimpleName();
    fileManager = new SimpleDB(dbName, SimpleDB.BLOCK_SIZE, SimpleDB.BUFFER_SIZE).fileManager();
  }

  @Test
  void canSaveValuesToFile() {
    // setup
    String fileName = "canSaveValuesToFile";
    BlockId blockId = new BlockId(fileName, 2);
    Page pageForWrite = new Page(fileManager.blockSize());
    int stringPosition = 88;
    String expectedString = "abcdefghijklm";
    int expectedInt = 345;
    int intPosition = stringPosition + Page.maxLength(expectedString.length());

    // invoke
    pageForWrite.setString(stringPosition, expectedString);
    pageForWrite.setInt(intPosition, expectedInt);
    fileManager.write(blockId, pageForWrite);

    // verify
    Page pageForRead = new Page(fileManager.blockSize());
    fileManager.read(blockId, pageForRead);
    assertEquals(expectedInt, pageForRead.getInt(intPosition));
    assertEquals(expectedString, pageForRead.getString(stringPosition));
  }
}
