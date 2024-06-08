package simpledb.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import simpledb.server.SimpleDB;

public class FileTest {

  private static final String DB_FILENAME = "dbtest";

  @AfterEach
  void teardown() {
    new File(DB_FILENAME).delete();
  }

  @Test
  void canSaveValuesToFile() {
    // setup
    FileManager fileManager =
        new SimpleDB(DB_FILENAME, SimpleDB.BLOCK_SIZE, SimpleDB.BUFFER_SIZE).fileManager();

    BlockId blockId = new BlockId("testfile", 2);
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
