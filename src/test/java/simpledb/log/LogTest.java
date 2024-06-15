package simpledb.log;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simpledb.file.Page;
import simpledb.server.SimpleDB;

public class LogTest {

  private static final String DB_FILENAME = "dbtest";

  LogManager logManager;
  PrintStream originalPrintStream;
  ByteArrayOutputStream logOutput;

  @BeforeEach
  void setup() {
    logManager = new SimpleDB(DB_FILENAME, SimpleDB.BLOCK_SIZE, SimpleDB.BUFFER_SIZE).logManager();

    originalPrintStream = System.out;
    logOutput = new ByteArrayOutputStream();
    System.setOut(new PrintStream(logOutput, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void teardown() {
    System.setOut(originalPrintStream);
  }

  @Test
  void canSaveRecordsToLogFile() {
    // setup and invoke
    createLogRecords(1, 70);
    printLogRecords("The log file now has these records:");

    // verify
    String actualLog = logOutput.toString();
    IntStream.range(1, 71)
        .forEach(
            i ->
                assertTrue(
                    actualLog.contains(STR."record\{i}")
                        && actualLog.contains(String.valueOf(i + 100))));
  }

  private void printLogRecords(String message) {
    System.out.println(message);

    Iterator<byte[]> iterator = logManager.iterator();
    while (iterator.hasNext()) {
      byte[] record = iterator.next();
      Page page = new Page(record);
      String string = page.getString(0);
      int numberPosition = Page.maxLength(string.length());
      int value = page.getInt(numberPosition);
      System.out.println(STR."[\{string}, \{value}]");
      ;
    }
    System.out.println();
  }

  private void createLogRecords(int start, int end) {
    System.out.print("creating records: ");
    for (int i = start; i <= end; i++) {
      byte[] record = createLogRecord(STR."record\{i}", i + 100);
      int logSequenceNumber = logManager.append(record);
      System.out.print(STR."\{logSequenceNumber} ");
    }
    System.out.println();
  }

  private byte[] createLogRecord(String string, int logSequenceNumber) {
    int numberPosition = Page.maxLength(string.length());
    byte[] record = new byte[Integer.BYTES + numberPosition];
    Page page = new Page(record);
    page.setString(0, string);
    page.setInt(numberPosition, logSequenceNumber);
    return record;
  }
}
