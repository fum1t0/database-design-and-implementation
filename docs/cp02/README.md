# Cp02. JDBC

データベースアプリケーションは API を利用することでデータベースエンジンと対話する。Java アプリケーションの場合は JDBC (Java DataBase Connectivity) と呼ばれる。JDBC ライブラリはほとんどが大規模な商用アプリケーションでのみ有用な高度な機能を実装した 5 つの Java パッケージで構成されている。この章では java.sql パッケージ内のコア機能に焦点を当てる。このコア機能は、基本的な仕様に必要なクラスやメソッドを含む部分と、便利さや柔軟性を提供するオプションの機能を含む高度な部分の 2 つに分けられる。

## 目次

- [2.1  基本的な JDBC](#21--基本的な-jdbc)
  - [2.1.1 データベースエンジンへの接続](#211-データベースエンジンへの接続)
  - [2.1.2 データベースエンジンからの切断](#212-データベースエンジンからの切断)
  - [2.1.3 SQLException](#213-sqlexception)
  - [2.1.4 SQL の実行](#214-sql-の実行)
  - [2.1.5 Result Sets](#215-result-sets)
  - [2.1.6 クエリのメタデータの使用](#216-クエリのメタデータの使用)
- [2.2 JDBC（応用編）](#22-jdbc応用編)
  - [2.2.1 ドライバーの隠蔽](#221-ドライバーの隠蔽)
    - [DriverManager の使用](#drivermanager-の使用)
    - [DataSource の使用](#datasource-の使用)
  - [2.2.2 明示的なトランザクション処理](#222-明示的なトランザクション処理)
  - [2.2.3 トランザクション分離レベル](#223-トランザクション分離レベル)
    - [例 1: 未コミットデータの読み取り](#例-1-未コミットデータの読み取り)
    - [例 2: 既存レコードの予期しない変更](#例-2-既存レコードの予期しない変更)
    - [例 3：レコードの数の予期しない変更](#例-3レコードの数の予期しない変更)
  - [2.2.4 プリペアドステートメント](#224-プリペアドステートメント)
  - [2.2.5 スクロール可能かつ更新可能な ResultSet](#225-スクロール可能かつ更新可能な-resultset)
  - [2.2.6 追加のデータ型](#226-追加のデータ型)
- [2.3 Java と SQL での計算の比較](#23-java-と-sql-での計算の比較)
- [2.4 サマリ](#24-サマリ)


## 2.1  基本的な JDBC

JDBCの基本機能は、Driver、Connection、Statement、ResultSet、および ResultSetMetadata の 5 つのインターフェースに具現化されている。さらに、これらのインターフェースのごくわずかなメソッドしか必須ではない。

```java
// Driver
public Connection connect(String url, Properties prop) throws SQLException;


// Connection
public Statement createStatement() throws SQLException;
public void      close()           throws SQLException;

// Statement
public ResultSet executeQuery(String qry)  throws SQLException;
public int       executeUpdate(String cmd) throws SQLException;
public void      close()                   throws SQLException;

// ResultSet
public boolean next()                  throws SQLException;
public int     getInt()                throws SQLException;
public String  getString()             throws SQLException;
public void    close()                 throws SQLException;
public ResultSetMetaData getMetaData() throws SQLException;

// ResultSetMetaData
public int    getColumnCount()              throws SQLException;
public String getColumnName(int column)     throws SQLException;
public int    getColumnType(int column)     throws SQLException;
public int getColumnDisplaySize(int column) throws SQLException;
```

### 2.1.1 データベースエンジンへの接続

各データベースエンジンには、クライアントとの接続のための独自のメカニズムがある。一方でクライアントはできるだけエンジンに接続する詳細を知りたくなく、クライアントが呼び出すためのクラスだけを提供してほしい。このようなクラスがドライバー。

JDBC ドライバークラスは [Driver インターフェース](https://docs.oracle.com/javase/jp/21/docs/api/java.sql/java/sql/Driver.html)を実装している。以下のコードでは Derby データベースへの接続を行うコード。
```java
String url = "jdbc:derby://localhost/testdb;create=true";
Driver d = new ClientDriver();
Connection conn = d.connect(url, null);
```
`Driver#connect` は 2 つの引数を取る。第一引数ではドライバー、サーバー、データベースを識別する URL を受け取る。この URL は接続文字列と呼ばれる。この接続文字列は 4 つの部分で構成されている。

- `jdbc:derby` はクライアントが使用するプロトコルを表している。この例では JDBC に対応した Derby クライアントであることを示している。
- `//localhost` はサーバが配置されているマシンを示している。
- `/testdb` はサーバ上のデータベースへのパスを示している。
- 接続文字列の残りの部分は、データベースエンジンに送信されるプロパティ。例えばユーザ認証を必要とする場合は以下のような形でユーザ名とパスワードも指定する。  
  `"jdbc:derby://localhost/testdb;create=true;user=einstein;password=emc2"`

Driver#connect の第 2 引数は [Properties 型](https://docs.oracle.com/javase/jp/21/docs/api/java.base/java/util/Properties.html)のオブジェクトを受け取る。サンプルコードでは全てのプロパティを接続文字列に含めているために null を渡しているが、代わりに第 2 引数経由で指定することもできる。
```java
String url = "jdbc:derby://localhost/testdb"; 
Properties prop = new Properties(); 
prop.put("create", "true"); 
prop.put("username", "einstein"); 
prop.put("password", "emc2");
Driver d = new ClientDriver(); 
Connection conn = d.connect(url, prop);
```

各データベースエンジンには独自の接続文字列構文がある。SimpleDB のサーバベースの接続文字列は Derby と異なり、プロトコルとマシン名のみを含む。ドライバークラスと接続文字列の構文はベンダー固有だが、JDBC プログラムの残りの部分は完全にベンダー中立となっている。変数 `d` と `conn` に対応する JDBC の型 [Driver](https://docs.oracle.com/javase/jp/21/docs/api/java.sql/java/sql/Driver.html), [Connection](https://docs.oracle.com/javase/jp/21/docs/api/java.sql/java/sql/Connection.html) はインターフェースとなっている。`conn` は `Driver#connect` によって返されたものであり、その実際のクラスはわからない。この状況は全ての JDBC プログラムに当てはまる。ドライバークラスの名前と接続文字列をのぞじて、JDBC プログラムはベンダー中立の JDBC インターフェースの身について知っており、関心を持っている。このため、基本的な JDBC クライアントは以下の 2 つのパッケージからインポートされる。

- ビルトインの java.sql パッケージ：ベンダー中立の JDBC インターフェースを取得するため
- ベンダーが提供するドライバークラスが含まれるパッケージ

### 2.1.2 データベースエンジンからの切断

クライアントがデータベースエンジンに接続している間、エンジンはクライアントの利用のためにリソースを割り当てることがある。例えば、クライアントはサーバからロックをリクエストして、他のクライアントがデータベースの一部にアクセスできないようにする。エンジンに接続できる能力さえもリソースと見做される。コネクションは貴重なリソースを保持しているため、データベースが不要になったらクライアントがエンジンから切断されることが期待される。クライアントは、以下のコードのようにその Connection オブジェクトの close メソッドを呼び出すことでエンジンから切断する。

```java
import java.sql.Driver;
import java.sql.Connection;
import org.apache.derby.jdbc.ClientDriver;

public class CreateTestDB {

  public static void main(String[] args) {
    String url = "jdbc:derby://localhost/testdb;create=true";
    Driver d = new ClientDriver();
    try {
      Connection conn = d.connect(url, null);
      System.out.println("Database Created");
      conn.close();
    } catch(SQLException e) {
      e.printStackTrace();
    }
  } 
}
```

### 2.1.3 SQLException

クライアントとデータベースエンジン間のやり取りは、多くの理由で例外を生成する可能性がある。例えば以下のような理由が挙げられる。

- クライアントがエンジンに、形式が正しくない SQL を実行するか、存在しないテーブルにアクセスするか、互換性のない値を比較する SQL を実行するよう要求する。
- エンジンが、それと同時にクライアントとの間でデッドロックが発生したためにクライアントを中止する。
- エンジンコードにバグがある。
- クライアントがエンジンにアクセスできない。ホスト名が間違っているか、ホストにアクセスできなくなった可能性がある。

様々なデータベースエンジンはこれらのレイガを処理するために独自の内部方法を持っている。例えば SimpleDB の場合は以下の通り。

| 原因         | 対応する例外                             |
| ------------ | ---------------------------------------- |
| ネットワーク | RemoteException                          |
| SQL 文       | BadSyntaxException                       |
| デッドロック | BufferAbortException, LockAbortException |
| サーバ       | RuntimeXception                          |

例外処理をベンダー非依存にするために、JDBC は独自の例外クラスである SQLException を提供する。データベースエンジンが内部例外に遭遇すると、それを SQLException でラップしてクライアントに送信する。SQLException に関連付けられているメッセージは、それを引き起こした内部例外を識別する。例えば Derby には 900 個のエラーメッ絵s−自我あるが、SimpleDB では全ての問題を「ネットワークの問題」、「不正な SQL ステートメント」、「サーバエラー」、「サポートされていない操作」、および 2 つの形式の「トランザクション中止」の 6 つのメッセージにまとめている。ほとんどの JDBC メソッドは SQLException を投げる。SQLException は検査例外であるため、クライアントはそれらを明治k亭にキャッチするか、さらに投げることによって明示的にそれらを処理する必要がある。先ほどのサンプルコードの `Driver#connect`, `Connection#close` はどちらも try-catch ブロックの中で実施され、どちらかが例外を引き起こすと、コードはスタックトレースを出力する。

先程のサンプルコードには例外が投げられたときに接続がクローズされていないという問題がある。これはリソースリークの例である。クライアントが終了したあと、エンジンはコネクションを簡単に回収することができない。以下のように try-with-resources 構文を利用すると、スコープを外れたときに自動的にリソースが解放される。
```java
try (Connection conn = d.connect(url, null)) {
  System.out.println("Database Created");
} catch (SQLException e) {
  e.printStackTrace();
}
```

### 2.1.4 SQL の実行

コネクションは、データベースエンジンとの「セッション」と考えることができる。このセッション中に、エンジンはクライアントのために SQL 文を実行する。JDBC ではこの考えた方を以下のような形でサポートしている。

Connection オブジェクトには Statement オブジェクトを返すcreateStatement メソッドがある。Statement オブジェクトには SQL を実行するために excuteQuery, executeUpdate という 2 つのメソッドがある。また、オブジェクトが持つリソースを開放するための close メソッドもある。

このコードは Amy の STUDENT レコードの MajorId を変更するために `Statement#executeUpdate` を呼び出すサンプルコードである。executeUpdate メソッドは変更内容の SQL を受け取り、更新されたレコード数を返却する。
```java
public class ChangeMajor {
  public static void main(String[] args) {
    String url = "jdbc:derby://localhost/studentdb";
    String cmd = "update STUDENT set MajorId=30 where SName='amy'";

    Driver d = new ClientDriver();
    try (Connection conn = d.connect(url, null);
         Statement stmt = conn.createStatement()) {
      int howmany = stmt.executeUpdate(cmd);
      System.out.println(howmany + " records changed.");
    } catch(SQLException e) {
      e.printStackTrace();
    }
  }
}
```

Connection オブジェクトと同様に Statement オブジェクトもクローズする必要がある。try-with-resources でどちらのオブジェクトも自動的にクローズされるようにすると良い。

SQL コマンドの仕様は興味深い点を示している。コマンドは Java の文字列として保存されるため、ダブルクォートを使用している。この区別により、引用符が 2 つの異なる意味を持つ可能性について心配する必要がなくなる。SQL の文字列はシングルクォートを、Java の文字列はダブルクォートを使用する。

ChangeMajor クラスは名前が `studentdb` のデータベースが存在することを前提としている。SimpleDB ディストリビューションにはデータベースを作成し、テーブルにレコードを挿入する CreateStudentDB クラスが含まれている。このクラスは、studentdb データベースを使用する際に最初に呼び出すべきプログラムである。以下のコードでは　STUDENT テーブルを作成し、それらに対してレコードを挿入する SQL を実行している。
```java
public class CreateStudentDB {
  public static void main(String[] args) {
    String url = "jdbc:derby://localhost/studentdb;create=true";
    Driver d = new ClientDriver();
    try (Connection conn = d.connect(url, null);
         Statement stmt = conn.createStatement()) {
      String s = "create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int)";
      stmt.executeUpdate(s);
      System.out.println("Table STUDENT created.");

      s = "insert into STUDENT(SId, SName,MajorId, GradYear) values ";
      String[] studvals = {
        "(1, 'joe', 10, 2021)",
        "(2, 'amy', 20, 2020)",
        "(3, 'max', 10, 2022)",
        "(4, 'sue', 20, 2022)",
        "(5, 'bob', 30, 2020)",
        "(6, 'kim', 20, 2020)",
        "(7, 'art', 30, 2021)",
        "(8, 'pat', 20, 2019)",
        "(9, 'lee', 10, 2021)"};
      for (int i=0; i<studvals.length; i++) {
        stmt.executeUpdate(s + studvals[i]);
      }
      System.out.println("STUDENT records inserted.");

      ...
    } catch(SQLException e) {
      e.printStackTrace();
    }
  }
}
```

### 2.1.5 Result Sets

`Statement#executeQuery` は SQL クエリを実行する。このメソッドへの引数は SQL クエリを示す文字列であり、ResultSet 型のオブジェクトを返却する。ResultSet オブジェクトはクエリの出力レコードである。クライアントはこれらのレコードを調べるために ResultSet を検索できる。

ResultSet の使用法を示す例として以下のコードを考える。
```java
public class StudentMajor {
  public static void main(String[] args) {
    String url = "jdbc:derby://localhost/studentdb";
    String qry = "select SName, DName from DEPT, STUDENT " + "where MajorId = DId";

    Driver d = new ClientDriver();
    try (Connection conn = d.connect(url, null);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(qry)) {
      System.out.println("Name\tMajor");
      while (rs.next()) {
        String sname = rs.getString("SName");
        String dname = rs.getString("DName");
        System.out.println(sname + "\t" + dname);
      }
    } catch(SQLException e) {
      e.printStackTrace();
    }
  }
}
```

この executeQuery の呼び出しは各学生の名前と専攻を含む ResultSet を返す。クライアントが ResultSet を取得したら、next メソッドを呼び出して出力レコードを反復処理する。このメソッドは次のレコードに移動し、移動が成功した場合は true を、もうレコードがない場合は false を返す。通常、クライアントはループを使用して全てのレコードを移動し、順番に各レコードを処理する。新しい ResultSet オブジェクトは常に最初のレコードの前に配置されているため、最初のレコードを見る前に next を呼び出す必要がある。このため、レコードをループするには以下のようなコードを擱筆ヨグあある。
```java
String qry = "select ...";
ResultSet rs = statement.executeQuery(qry);
while (rs.next()) {
  ... // レコードを処理
}
```

ResultSet はエンジン上で貴重なリソースを結びつける。 close メソッドはこれらのリソースを解放し、他のクライアントに利用可能にする。したがって、クライアントはできるだけ早く ResultSet を閉じることを心がけるべきである。

### 2.1.6 クエリのメタデータの使用

ResultSet のスキーマは、各フィールドの名前、型、表示サイズとして定義される。この情報は [ResultSetMetaData インターフェース](https://docs.oracle.com/javase/jp/21/docs/api/java.sql/java/sql/ResultSetMetaData.html) を介して利用できる。クライアントがクエリを実行するとき、通常、出力テーブルのスキーマを知っている。例えば StudentMajor クライアントにハードコードされているのは、その ResultSet に 2 つの文字列フィールド `SName` と `DName` が含まれているという知識である。

クライアントがユーザにクエリを入力として送信する場合を感gなえる。プログラムはクエリの ResultSet で getMetaData メソッドを呼び出し、ResultSetMetaData 型のオブジェクトを返す。その後、このオブジェクトのメソッドを用いて、出力テーブルのスキーマを判断できる。以下は引数の ResultSet のスキーマを出力するために ResultSetMetaData を利用している。
```java
void printSchema(ResultSet rs) throws SQLException {
  ResultSetMetaData md = rs.getMetaData();
  for(int i=1; i<=md.getColumnCount(); i++) {
    String name = md.getColumnName(i);
    int size = md.getColumnDisplaySize(i);
    int typecode = md.getColumnType(i);
    String type;
    if (typecode == Types.INTEGER) {
      type = "int";
    } else if (typecode == Types.VARCHAR) {
      type = "string";
    } else {
      type = "other";
    }
    System.out.println(name + "\t" + type + "\t" + size);
  }
}
```
このコードは ResultSetMetaData オブジェクトの典型的な使用法を示している。まず、getColumnCount メソッドを呼び出して ResultSet 内のフィールドの数を返し、次に getColumnName, getColumnType, getColumnDisplaySize メソッドを利用して各列の名前、型、サイズを判断する。列の番号は 0 ではなく 1 から始まるようになっている。

getColumnType メソッドはフィールドの型をエンコードした整数を返す。これらのコードは JDBC の Types クラス内の定数として定義されている。このクラスには 30 種類の異なる型のコードが含まれている。これらの型の実際の値は重要ではなく、JDBC は常にコード名を値ではなく参照する必要がある。

メタデータの知識が必要なクライアントの良い例は以下のようなコマンドインタプリタである。
```java
public class SimpleIJ {
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    System.out.println("Connect> ");
    String s = sc.nextLine();
    Driver d = (s.contains("//")) ? new NetworkDriver() : new EmbeddedDriver();

    try (Connection conn = d.connect(s, null);
         Statement stmt = conn.createStatement()) {
      System.out.print("\nSQL> ");
      while (sc.hasNextLine()) {
        // process one line of input
        String cmd = sc.nextLine().trim();
        if (cmd.startsWith("exit")) {
          break;
        } else if (cmd.startsWith("select")) {
          doQuery(stmt, cmd);
        } else {
          doUpdate(stmt, cmd); System.out.print("\nSQL> ");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    sc.close();
   }

  private static void doQuery(Statement stmt, String cmd) {
    try (ResultSet rs = stmt.executeQuery(cmd)) {
      ResultSetMetaData md = rs.getMetaData();
      int numcols = md.getColumnCount();
      int totalwidth = 0;

      // print header
      for(int i=1; i<=numcols; i++) {
        String fldname = md.getColumnName(i);
        int width = md.getColumnDisplaySize(i);
        totalwidth += width;
        String fmt = "%" + width + "s";
        System.out.format(fmt, fldname);
      }
      
      System.out.println();
      for(int i=0; i<totalwidth; i++) {
        System.out.print("-");
      }
      System.out.println();

      // print records
      while(rs.next()) {
        for (int i=1; i<=numcols; i++) {
          String fldname = md.getColumnName(i);
          int fldtype = md.getColumnType(i);
          String fmt = "%" + md.getColumnDisplaySize(i);
          if (fldtype == Types.INTEGER) {
            int ival = rs.getInt(fldname);
            System.out.format(fmt + "d", ival);
          } else {
            String sval = rs.getString(fldname);
            System.out.format(fmt + "s", sval);
          }
        }
        System.out.println();
      }
    } catch (SQLException e) {
      System.out.println("SQL Exception: " + e.getMessage());
    }
  }
  
  private static void doUpdate(Statement stmt, String cmd) {
    try {
      int howmany = stmt.executeUpdate(cmd);
      System.out.println(howmany + " records processed");
    } catch (SQLException e) {
      System.out.println("SQL Exception: " + e.getMessage());
    }
  }
}
```

main メソッドはユーザからの接続文字列を読み取り、適切なドライバーを使用するかどうかを判断する。コードは接続文字列内で `//` を探す。これらの未z列が表示される場合、文字列はサーバーベースの接続を指定している必要があり、それ以外の場合は埋め込み接続である。その後、メソッドは接続文字列を適切なドライバーの connect メソッドに渡して接続を確立する。その後、while ループの各イテレーションで 1 行のテキストを処理する。テキストが SQL である場合は doQuery または doUpdate が適切に呼び出される。ユーザは `exit` と入力することでループを終了し、プログラムが終了する。

doQuery メソッドはクエリを実行し、出力テーブルの ResultSet とメタデータを取得する。メソッドの殆どは、値の適切な出力間隔を決定することに関係している。getColumnDisplaySize への呼び出しは各フィールドのスペース要件を返す。コードはこれらの数値を利用して、フィールド値が適切に整列するようにフォーマット文字列を構築する。このコードの複雑さは「悪魔は細部に宿る」という考え方を示している。つまり、概念的に難しいタスクは ResultSet, ResultSetMetaData のおかげで簡単にコーディングできるが、データを整列させる些細なタスクがほとんどのコーディング労力を占める。

doQuery, doUpdate メソッドはエラーが発生した際、エラーメッセージを出力する以外の処理を行わない。このエラー処理戦略により、ユーザが `exit` コマンドを入力するまで、メインループがステートメントを受け入れ続けることができる。

## 2.2 JDBC（応用編）

基本的な JDBC は比較的簡単に利用可能だが、データベースエンジンとのやり取りの方法はかなり限定される。ここではデータベースへのアクセス方法をより制御するための JDBC の追加機能を取り扱う。

### 2.2.1 ドライバーの隠蔽

基本的な JDBC では、クライアントは Driver オブジェクトのインスタンスを取得し、その connect メソッドを呼び出すことでデータベースエンジンに接続する。この戦略の問題点は、ベンダー固有のコードがクライアントプログラムに配置されることである。JDBC にはドライバー情報をクライアントプログラム
から隠すためのベンダー中立のクラスとして DriverManager と DataSource がある。

#### DriverManager の使用

DriverManager クラスは複数のドライバーを保持している。指定された接続文字列を処理できるドライバーをコレクションから検索するための静的メソッドが含まれている。これらのうちの 2 つを以下に示す。
```java
static public void registerDriver(Driver driver) throws SQLException;
static public Connection getConnection(String url, Properties p) throws SQLException;
```

アイデアとしては、クライアントが使用する可能性のある各データベースのドライバーを登録するためにクライアントが繰り返し registerDriver を呼び出すことが考えられる。クライアントがデータベースに接続する必要があるときは、getConnection を呼び出す際に接続文字列のみを提供する。DriverManager はコレクション愛の各ドライバーに接続文字列を試し、そのうち 1 つが null 以外の Connection オブジェクトを返すまで続ける。

例えば以下のようなコードを考える。最初の 2 行で Derby と SimpleDB ドライバーを DriverManager に登録する。最後の 2 行は Derby サーバに接続を確立する。クライアントは getConnection を呼び出すときにドライバーを指定する必要はなく、接続文字列のみを指定する。DriverManager は登録されているドライバーのうちどれを使用するかを決定する。
```java
DriverManager.registerDriver(new ClientDriver());
DriverManager.registerDriver(new NetworkDriver());
String url = "jdbc:derby://localhost/studentdb";
Connection c = DriverManager.getConnection(url);
```

このコードは registerDriver メソッドの実行時にドライバーを指定しているため、ドライバー情報を隠蔽しきれていない。JDBC は、ドライバーが java システムプロパティファイルで指定されるようにすることでこの問題を解決している。例えば Derby と SimpleDB ドライバーは以下をプロパティファイルに追記することで登録できる。
```properties
jdbc.drivers=org.apache.derby.jdbc.ClientDriver:simpledb.remote.NetworkDriver
```

ドライバー情報をプロパティファイルに配置することは、クライアントコードからドライバーの仕様を削除するための方法である。このファイルを変更するだけで、再コンパイルすることなく全ての JDBC クライアントで使用されるドライバー情報を修正できる。

#### DataSource の使用

DriverManager は JDBC クライアントからドライバーを隠すことができるが、接続文字列を隠すことはできない。特に、先程のサンプルコードには `jdbc:derby` という文字列が含まれているため、どのドライバーを意図しているかが明らかである。JDBC の最近の追加機能の 1 つは、[javax.sql パッケージの DataSource インターフェース](https://docs.oracle.com/javase/jp/21/docs/api/java.sql/javax/sql/DataSource.html)である。これは、現在ドライバーを管理するうえで推奨されている戦略である。

DataSource オブジェクトはドライバーと接続文字列の両方をカプセル化し、クライアントが接続の詳細を知ることなくエンジンに接続できるようにする。Derby でデータソースを作成するには ServerDataSource と EmbeddedDataSource という Derby が提供するクラスが必要で、どちうらも DataSource を実装している。クライアントコードとしては以下が考えられる。
```java
ClientDataSource ds = new ClientDataSource();
ds.setServerName("localhost");
ds.setDatabaseName("studentdb");
Connection conn = ds.getConnection();
```

各データベースベンダーは DataSource を実装する独自のクラスを提供している。これらのクラスはベンダー固有だが、ドライバー名や接続文字列の構文など、そのドライバーの詳細をカプセル化することができる。これらを使用するプログラムは必要な値のみを指定すればよくなる。

DataSource を利用する利点の 1 つは、クライアントがもはやドライバーの名前や接続文字列の構文を知る必要がなくなることである。しかし、このクラスは依然としてベンダー固有であり、したがってクライアントコードは完全にベンダー非依存ではない。この問題を解決するには様々な方法がある。1 つは、データベース管理者が DataSource オブジェクトをファイルに保存することである。DBA はオブジェクトを作成し、Java シリアル化を使用してファイルに書き込むことができる。クライアントはそのファイルを読み取り、DataSource オブジェクトに戻すために逆シリアル化を行うことでデータソースを取得できる。この解決策はプロパティファイルを使用するのと似ている。DataSource オブジェクトがファイルに保存されると、それを使用して任意の JDBC クライアントで使用できる。また、DBA はそのファイルの内容を簡単に置き換えることでデータソースを変更できる。

2 番目の解決策はファイルの代わりに名前サーバ（JNDI サーバなど）を使用することである。DBA は DataSource オブジェクトを名前サーバ二配置し、クライアントはサーバから DataSource を要求する。名前サーバは多くのコンピューティング環境の一般的な部分であるため、この解決策はしばしば容易に実装できるが、詳細はここでは取り扱わない。

### 2.2.2 明示的なトランザクション処理

各 JDBC クライアントはトランザクションの一連と実行として実行される。概念的にはトランザクションは「作業単位」であり、そのデータベースの全ての相互作用が単位として扱われる。例えばトランザクション内の 1 つの更新が失敗した場合、エンジンはそのトランザクションによって行われた全ての更新が失敗することを保証する。トランザクションはその現在の作業単位が正常に完了したときにコミットされる。データベースエンジンはすべての変更を永続化し、そのトランザクションに割り当てられたリソース（ロックなど）を開放することでコミットを実装する。コミットが完了すると、エンジンは新しいトランザクションを開始する。トランザクションは、コミットできない場合にロールバックされる。データベースエンジンはロールバックを実装することで、そのトランザクションで行われた全ての変更を元に戻し、ロックを解放し、新しいトランザクションを開始する。コミットまたはロールバックされたトランザクションは完了したと言われる。

トランザクションは基本的な JDBC では暗黙的に処理される。データベースエンジンはトランザクションの協会を選択肢、いつトランザクションをコミットすべきか、それをロールバックすべきか決定する。これはオートコミットと呼ばれる。オートコミット中、エンジンはそれぞれの SQL ステートメントを独自のトランザクションで実行する。ステートメントが正常に完了した場合、エンジンはトランザクションをコミットし、それ以外の場合はトランザクションをロールバックする。更新コマンドは executeUpdate メソッドが完了したときにすぐに完了し、クエリはクエリの ResultSet が閉じられたときに完了する。

トランザクションはロックを蓄積し、そのトランザクションがコミットまたはロールバックされるまで開放されない。これらのロックは他のトランザクションを待機させる可能性があるため、短いトランザクションがより多くの並行性を可能にする。この原則は、オートコミットモードで実行されているクライアントが ResultSet を早めに閉じるべきであることを示唆している。

オートコミットは JDBC クライアントの合理的なデフォルトモードである。1 つの SQL ステートメントごとに 1 つのトランザクションを持つことは短いトランザクションをもたらし、しばしば適切な方法である。ただし、トランザクションが複数の SQL ステートメントで構成される必要がある場合もある。

オートコミットが望ましくない状況の 1 つは、クライアントが同時に 2 つのステートメントをアクティブにする必要がある場合である。例として以下のようなコードを考えてみる。
```java
DataSource ds = ...
Connection conn = ds.getConnection();
Statement stmt1 = conn.createStatement();
Statement stmt2 = conn.createStatement();
ResultSet rs = stmt1.executeQuery("select * from COURSE");
while (rs.next()) {
  String title = rs.getString("Title");
  boolean goodCourse = getUserDecision(title);
  if (!goodCourse) {
    int id = rs.getInt("CId");
    stmt2.executeUpdate("delete from COURSE where CId =" + id);
  }
}
rs.close();
```

このコードはまず全てのコースを取得するクエリを実行する。その後、ResultSet をループし、各コースを削除するかどうかユーザに尋ねる。その後、削除するために `delete` 文を実行する。このコードの問題点は、ResultSet がまだオープンの状態で `delete` 文が実行されることになる。接続は同時に 1 つのトランザクションのみをサポートするため、新しいトランザクションを作成する前にクエリのトランザクションをコミットする必要がある。そして、クエリのトランザクションがコミットされているため、残りのレコードセットにアクセスすることは自然ではない。コードは例外を投げるか、予測できない動作をするだろう。

オートコミットが望ましくない場合は、データベースへの複数の変更をまとめて行う必要がある場合である。以下のコードはその例である。
```java
DataSource ds = ...
Connection conn = ds.getConnection();
Statement stmt = conn.createStatement();
String cmd1 = "update SECTION set Prof= 'brando' where SectId = 43";
String cmd2 = "update SECTION set Prof= 'einstein' where SectId = 53";
stmt.executeUpdate(cmd1);
// suppose that the engine crashes at this point
stmt.executeUpdate(cmd2);
```

このコードの意図はセクション 43 と 53 を教える教授を交換することである。ただし、最初の executeUpdate の呼び出し後、2 番目の呼び出し前にエンジンがクラッシュした場合、データベースが正しくなくなる。このコードでは、両方の SQL ステートメントを同じトランザクションで実行する必要があるため、それらが一緒にコミットされるか、一緒にロールバックされる必要がある。

オートコミットモードはまた、不便な場合がある。プログラムがテキストファイルからデータを読み込むなど、複数の挿入を行っている場合を考えると、プログラムの実行中にエンジンがクラッシュすると、一部のレコードが insert され、他のレコードが insert されない可能性がある。プログラムがどこで失敗したかを特定し、欠落したレコードのみを挿入するようにプログラムを書き直すことは面倒で時間がかかる場合がある。より良い代替案は、全ての inset 文を同じトランザクションに配置することである。その後、システムクラッシュ後、それら全てがロールバックされ、クライアントを単純再実行することができる。

Connection インターフェースにはクライアントが自分のトランザクションを明示的に処理するための以下の 3 つのメソッドが含まれている。
```java
public void setAutoCommit(boolean ac) throws SQLException;
public void commit()                  throws SQLException;
public void rollback()                throws SQLException;
```

クライアントは `setAutoCommit(false)` を呼び出すことでオートコミットをオフにする。クライアントは必要に応じて `commit` または `rollback` を呼び出すことで現在のトランザクションを完了し、新しいトランザクションを開始する。

クライアントがオートコミットをオフにすると、失敗した SQL ステートメントをロールバックする責任を負う。特に、トランザクション中に例外が投げられた場合、クライアントはそのトランザクションを例外処理コード内でロールバックする必要がある。例として先ほどのコースを教える教授を差し替えるコードを考える。それを修正したものが以下のコードである。
```java
DataSource ds = ...
try (Connection conn = ds.getConnection()) {
  conn.setAutoCommit(false);
  Statement stmt = conn.createStatement();
  ResultSet rs = stmt.executeQuery("select * from COURSE");
  while (rs.next()) {
    String title = rs.getString("Title");
    boolean goodCourse = getUserDecision(title);
    if (!goodCourse) {
      int id = rs.getInt("CId");
      stmt.executeUpdate("delete from COURSE where CId =" + id);
    }
  }
  rs.close();
  stmt.close();
  conn.commit();
} catch (SQLException e) {
  e.printStackTrace();
  try {
    if (conn != null) {
      conn.rollback();
    }
  } catch (SQLException _) {}
}
```

このコードは Connection が作成された直後に setAutoCommit を呼び出し、ステートメントが完了した直後に即座に commit を呼び出している。catch ブロックには rollback の呼び出しが含まれている。この呼び出しには例外を投げる可能性があるため、自分自身の try ブロック内に配置する必要がある。ロールバック中に例外が発生すうるとデータベースが破損する可能性があるように見えるが、データベースのロールバックアルゴリズムはそのような可能性を処理するように設計されている。この詳細は第 5 章で扱う。したがって、このコードはデータベースエンジンが問題を解決することを知っているため、失敗したロールバックを無視してもよい。

### 2.2.3 トランザクション分離レベル

データベースサーバには通常、複数のクライアントが同時にアクティブであり、それぞれが独自のトランザクションを実行している。これらのトランザクションを同時に実行することで、サーバはスループットとレスポンスタイムを向上させることができる。したがって、並行性はとても良いものである。ただし、制御されていない並行性は問題を引き起こす可能性がある。なぜなら、トランザクションが他のトランザクション二鑑賞し、その他のトランザクションが予期しない方法で使用するデータを変更することができるからである。以下に、発生する可能性のある問題の種類を示す 3 つの例を示す。

#### 例 1: 未コミットデータの読み取り

このコードを再考し、2 つのセクションの教授を入れ替えるコードとして、単一のトランザクション（つまり、オートコミットをオフにした状態）として実行されると仮定する。このトランザクションを T1 とする。また、大学が教授にコースの数に基づいてボーナスを与えることに決定したと仮定し、各教授が教えたセクションの数を数えるトランザクション T2 を実行する。さらに、これらの 2 つのトランザクションが偶然同時に実行されるとする。その結果、ブランドー教授とアインシュタイン教授は、それぞれ 1 つ余分なコースと 1 つ少ないコースが与えられることになり、それが彼らのボーナスに影響を与えてしまう。
```java
DataSource ds = ...
Connection conn = ds.getConnection();
Statement stmt = conn.createStatement();
String cmd1 = "update SECTION set Prof= 'brando' where SectId = 43";
String cmd2 = "update SECTION set Prof= 'einstein' where SectId = 53";
stmt.executeUpdate(cmd1);
// suppose that the engine crashes at this point
stmt.executeUpdate(cmd2);
```

何が問題だったのか？それぞれのトランザクションはそれぞれでは正しいが、一緒に実行すると大学が間違ったボーナスを与える原因になる。問題は T2 が読み取ったレコードが一貫していると誤って仮定したことである。つまり、それらが一緒に意味を持っていると仮定したのである。しかし、未コミットのトランザクションによって書き込まれたデータは常に一貫しているとは限らない。T1 の場合、不一致は 2 つの変更が行われた時点で発生した。T2 がその時点で未コミットの変更されたレコードを読み取ったとき、その不一致により、誤った計算が行われた。

#### 例 2: 既存レコードの予期しない変更

この例では、STUDENTテーブルにMealPlanBalというフィールドが含まれており、これは学生が食堂で食べ物を買うために持っている金額を示している。この 2 つのトランザクションを考えてみる。
```java
// Transaction T1
DataSource ds = ...
Connection conn = ds.getConnection();
conn.setAutoCommit(false);
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("select MealPlanBal from STUDENT where SId = 1");
rs.next();
int balance = rs.getInt("MealPlanBal");
rs.close();

int newbalance = balance 10;
if (newbalance < 0) {
  throw new NoFoodAllowedException("You cannot afford this meal");
}

stmt.executeUpdate("update STUDENT set MealPlanBal = " + newbalance + " where SId = 1");
conn.commit();

// Transaction T2
DataSource ds = ...
Connection conn = ds.getConnection();
conn.setAutoCommit(false);
Statement stmt = conn.createStatement();
stmt.executeUpdate("update STUDENT set MealPlanBal = MealPlanBal + 1000 where SId = 1");
conn.commit();
```

トランザクション T1 は、ジョーが 10 ドルの昼食を買ったときに実行される。このトランザクションは、現在の残高を調べるためのクエリを実行し、残高が十分であることを確認し、適切に残高を減らす。トランザクション T2 は、ジョーの両親が送った 1000 ドルのチェックを彼の食事プランの残高に追加するために実行される。そのトランザクションは単純に、ジョーの残高を増やすための `update` 文を実行する。
さて、これらの 2 つのトランザクションが偶然同時に実行され、ジョーの残高が 50 ドルの場合を考えてみる。特に、T2 が T1 の rs.close を呼び出した直後に開始され、完了する。その後、T2 が最初にコミットされ、残高を 1050 ドルに変更する。しかし、T1 はこの変更を知らず、まだ残高が 50 ドルであると考えている。したがって、T1 は残高を 40 ドルに変更し、コミットする。その結果、1000 ドルの預金が彼の残高に反映されず、つまり、更新が「失われた」。
問題は、トランザクションT1が、T1が値を読み取った時点とT1が値を変更した時点の間に、食事プランの残高の値が変わらないと誤って仮定したことである。形式的には、この仮定はリピータブルリードと呼ばれる。なぜなら、トランザクションがデータベースから項目を繰り返し読み取っても常に同じ値が返されると仮定するからである。

#### 例 3：レコードの数の予期しない変更

大学の食堂サービスが昨年 10 万ドルの利益を上げた。大学は学生に過剰請求したことを気にして、利益を学生全員で均等に分配することに決定した。つまり、1000 人の現在の学生がいる場合、大学は各食事プランの残高に 100 ドルを追加する。このコードは以下である。
```java
DataSource ds = ...
Connection conn = ds.getConnection();
conn.setAutoCommit(false);
Statement stmt = conn.createStatement();
String qry = "select count(SId) as HowMany from STUDENT where GradYear >= extract(year, current_date)";
ResultSet rs = stmt.executeQuery(qry);
rs.next();
int count = rs.getInt("HowMany");
rs.close();

int rebate = 100000 / count;
String cmd = "update STUDENT set MealPlanBalance = MealPlanBalance + " + rebate + " where GradYear >= extract(year, current_date)";
stmt.executeUpdate(cmd);
conn.commit();
```

このトランザクションの問題点は、払い戻し額の計算と STUDENT レコードの更新の間に現在の学生数が変化しないと仮定していることである。しかし、レコードセットのクローズと更新ステートメントの実行の間にデータベースに複数の新しい STUDENT レコードが挿入されたとする。これらの新しいレコードは、事前計算された払い戻しを誤って受け取り、大学は 10 万ドル以上の払い戻しを支払うことになる。これらの新しいレコードは、トランザクションが開始された後に不可解に現れるため、ファントムレコードと呼ばれる。

これらの例は、2 つのトランザクションが相互作用する際に発生する可能性のある問題を示している。任意のトランザクションが問題を起こさないことを保証する唯一の方法は、他のトランザクションから完全に分離して実行することである。この形式の分離をシリアライズ可能性と呼び、第 5 章で詳細に議論されている。

残念ながら、シリアライズ可能なトランザクションは非常に遅くなる場合がある。なぜなら、データベースエンジンが許可する並行性の量を大幅に減らす必要があるためである。そのため、JDBC はトランザクションが持つべき分離のレベルを指定できる 4 つの分離レベルを定義している。

- Read-Uncommitted 分離は、まったく分離されていない。このようなトランザクションは、上記の 3 つの例のいずれかの問題に直面する可能性がある。
- Read-Committed 分離は、トランザクションが未確定の値にアクセスすることを禁止する。繰り返し読み込みとファントムレコードの関連する問題は依然として発生する可能性がある。
- Repeatable-Read 分離は、リードが常に繰り返し可能であるように、読み取り確定を拡張する。可能な問題はファントムレコードによるものだけである。
- Serializable 可能な分離は、問題が決して発生しないことを保証する。

JDBC クライアントは、`Connection#setTransactionIsolation` を呼び出すことで、望む分離レベルを指定する。たとえば、次のコードは分離レベルをシリアライズ可能に設定する。
```java
DataSource ds = ...
Connection conn = ds.getConnection();
conn.setAutoCommit(false);
conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
```

これらの 4 つの分離レベルは、実行速度と潜在的な問題の間のトレードオフを示している。つまり、トランザクションの実行速度が速いほど、トランザクションが誤って実行される可能性が高くなる。このリスクは、クライアントの慎重な分析によって緩和することができる。例えば、幻のレコードやリピータブルリードが問題にならないと自信を持つことができるかもしれない。これは、トランザクションが insert のみを実行するか、特定の既存レコードを delete する場合（たとえば、`delete from STUDENT where SId = 1` のように）である場合である。この場合、読み取り確定の分離レベルは速くて正確である。

もう 1 つの例として、潜在的な問題が興味深くないということを自信を持つかもしれない。トランザクションが年ごとに、その年に与えられた平均成績を計算する場合を想定する。トランザクションの実行中に成績の変更が発生する可能性はあるとしても、それらの変更が結果の統計に大きな影響を与える可能性は低いと判断する。この場合、読み取り確定または読み取り未確定の分離レベルを選択することができる。

多くのデータベースサーバー（Derby、Oracle、および Sybase を含む）のデフォルトの分離レベルは読み取り確定である。このレベルは、オートコミットモードで単純なクエリを実行する初心者のユーザによって提案されるクエリに適している。ただし、クライアントプログラムが重要なタスクを実行する場合は、それに適切な分離レベルを慎重に決定することが同じくらい重要である。オートコミットモードをオフにするプログラマーは、各トランザクションの適切な分離レベルを選択する際に非常に注意しなければならない。

### 2.2.4 プリペアドステートメント

多くの JDBC クライアントプログラムは、ユーザーから引数の値を受け取り、その引数に基づいてSQLステートメントを実行するという意味で、パラメーター化されている。そのようなクライアントの例は、以下に示すデモクライアント FindMajors である。
```java
public class FindMajors {
  public static void main(String[] args) {
    System.out.print("Enter a department name: ");
    Scanner sc = new Scanner(System.in);
    String major = sc.next();
    sc.close();
    String qry = "select sname, gradyear from student, dept where did = majorid and dname = '" + major + "'";

    ClientDataSource ds = new ClientDataSource();
    ds.setServerName("localhost");
    ds.setDatabaseName("studentdb");
    try (Connection conn = ds.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(qry)) {
      System.out.println("Here are the " + major + " majors");
      System.out.println("Name\tGradYear");
      while (rs.next()) {
        String sname = rs.getString("sname");
        int gradyear = rs.getInt("gradyear");
        System.out.println(sname + "\t" + gradyear);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
```
このクライアントは、最初にユーザーに部門名を尋ねる。それからこの名前を、実行する SQL クエリに組み込む。たとえば、ユーザーが値 `math` を入力したとする。その場合、生成される SQL クエリは `select SName, GradYear from STUDENT, DEPT where DId = MajorId and DName = 'math'` となる。

クエリを生成する際に、コードが部門名を囲む単一引用符を明示的に追加する方法に注目すると、このように動的に SQL ステートメントを生成する代わりに、クライアントはパラメーター化された SQL ステートメントを使用することができる。パラメーター化されたステートメントは、`'?'` 文字が欠落しているパラメーター値を示す SQL ステートメントである。ステートメントには複数のパラメーターがあり、すべてが `'?'` で示される。各パラメーターには、文字列内の位置に対応するインデックス値がある。たとえば、パラメーター化されたステートメント `delete from STUDENT where GradYear = ? and MajorId = ?` は、まだ指定されていない卒業年度と専攻を持つすべての学生を削除する。GradYear の値はインデックス 1 に、MajorId の値はインデックス 2 に割り当てられる。

JDBC クラス PreparedStatement は、パラメーター化されたステートメントを処理する。
クライアントは、準備されたステートメントを次の 3 つのステップで処理する：
- 指定されたパラメーター化された SQL ステートメント用の PreparedStatement オブジェクトを作成する。
- パラメーターに値を割り当てる。
- 準備されたステートメントを実行する。

たとえば以下のコードでは、FindMajors クライアントをパラメータ化されたステートメントを使用するように修正している。
```java
public class PreparedFindMajors {
  public static void main(String[] args) {
    System.out.print("Enter a department name: ");
    Scanner sc = new Scanner(System.in);
    String major = sc.next();
    sc.close();
    String qry = "select sname, gradyear from student, dept " + "where did = majorid and dname = ?";  // fix
    ClientDataSource ds = new ClientDataSource();
    ds.setServerName("localhost");
    ds.setDatabaseName("studentdb");
    try (Connection conn = ds.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(qry)) {  // fix
      pstmt.setString(1, major);  // fix
      ResultSet rs = pstmt.executeQuery();  // fix
      System.out.println("Here are the " + major + " majors");
      System.out.println("Name\tGradYear");
      while (rs.next()) {
        String sname = rs.getString("sname");
        int gradyear = rs.getInt("gradyear");
        System.out.println(sname + "\t" + gradyear);
      }
      rs.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
```

変更点はコメントで示されている。コメントが入った最後の 3 つのステートメントは、上記の 3 つの項目に対応している。まず、クライアントは prepareStatement メソッドを呼び出して PreparedStatement オブジェクトを作成し、パラメーター化された SQL ステートメントを引数として渡す。次に、クライアントは setString メソッドを呼び出して最初のパラメーターに値を割り当てる。最後に、executeQuery メソッドを呼び出してステートメントを実行する。

以下は、最も一般的な PreparedStatement メソッドの API を示している。
```java
public ResultSet executeQuery()              throws SQLException;
public int executeUpdate()                   throws SQLException;
public void setInt(int index, int val)       throws SQLException;
public void setString(int index, String val) throws SQLException;
```

executeQuery および executeUpdate メソッドは、Statement の対応するメソッドに類似している。違いは、引数が必要ないことである。setInt, setString メソッドは、パラメーターに値を割り当てる。先程のサンプルコードでは、setString の呼び出しで部門名が最初のインデックスパラメーターに割り当てられた。setString メソッドは値の周りに自動的に単一引用符を挿入するので、クライアントがそれを行う必要はない。

多くの人々は、SQL ステートメントを明示的に作成するよりも、準備されたステートメントを使用する方が便利だと考えている。以下のコードのようにステートメントがループ内で生成される場合、準備されたステートメントもより効率的なオプションである。その理由は、データベースエンジンがパラメーターの値を知らなくても準備されたステートメントをコンパイルできるからである。ステートメントは一度コンパイルされ、その後、ループ内で再コンパイルすることなく繰り返し実行される。
```java
// Prepare the query
String qry = "select SName, GradYear from STUDENT, DEPT " + "where DId = MajorId and DName = ?"; PreparedStatement pstmt = conn.prepareStatement(qry);
// Repeatedly get parameters and execute the query
String major = getUserInput();
while (major != null) {
  pstmt.setString(1, major);
  ResultSet rs = pstmt.executeQuery();
  displayResultSet(rs);
  major = getUserInput();
}
```

### 2.2.5 スクロール可能かつ更新可能な ResultSet 

基本的な JDBC の ResultSet は、前方のみにスクロール可能かつ更新は不可能である。フル JDBC では、ResultSet をスクロール可能かつ更新可能にすることもできる。クライアントは、そのような ResultSet を任意のレコードに配置し、現在のレコードを更新し、新しいレコードを挿入することができる。以下はこれらの追加メソッドの API を示している。
```java
// Methods used by scrollable result sets
public void    beforeFirst()        throws SQLException;
public void    afterLast()          throws SQLException;
public boolean previous()           throws SQLException;
public boolean next()               throws SQLException;
public boolean absolute(int pos)    throws SQLException;
public boolean relative(int offset) throws SQLException;

// Methods used by updatable result sets
public void updateInt(String fldname, int val) throws SQLException;
public void updateString(String fldname, String val) throws SQLException;
public void updateRow()       throws SQLException;
public void deleteRow()       throws SQLException;
public void moveToInsertRow() throws SQLException;
```

beforeFirst メソッドは、ResultSet を最初のレコードの前に配置し、afterLast メソッドは、ResultSet を最後のレコードの後に配置する。absolute メソッドは、ResultSet を指定されたレコードに正確に配置し、そのようなレコードがない場合は false を返す。relative メソッドは、相対的な行数で ResultSet を配置する。特に、relative(1) は next と同じであり、relative(-1) は previous と同じである。

updateInt および updateString メソッドは、クライアント上の現在のレコードの指定されたフィールドを変更する。ただし、変更はupdateRow が呼び出されるまでデータベースに送信されない。updateRow を呼び出す必要性はやや不格好であるが、これにより JDBC がレコードの複数のフィールドの更新をエンジンへの単一の呼び出しでバッチ処理できるようになる。

insert は、挿入行という概念によって処理される。この行はテーブルに存在せず（たとえば、それにスクロールできない）、新しいレコードのステージングエリアとして機能しする。クライアントは、moveToInsertRow を呼び出して ResultSet を挿入行に配置し、その後、updateXXX メソッドを呼び出してそのフィールドの値を設定し、データベースにレコードを挿入し、最後に moveToCurrentRow を呼び出してレコードセットを挿入前の状態に再配置する。

デフォルトでは、レコードセットは前方のみで更新不可能である。クライアントがより強力なレコードセットを必要とする場合は、Connection のcreateStatement メソッドでそれを指定する。基本的な JDBC の引数なしの createStatement メソッドに加えて、スクロール可能性と更新可能性をクライアントが指定する2つの引数メソッドもある。例として次のステートメントを考えてみる。
```java
Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
```

このステートメントから生成されたすべての ResultSet は、スクロール可能かつ更新可能になる。定数 TYPE_FORWARD_ONLY はスクロールできない ResultSet を、CONCUR_READ_ONLY は更新できない ResultSet を指定する。これらの定数は組み合わせて、必要なスクロール性と更新性を得ることができる。

例として、先ほどのコードを考える。
```java
DataSource ds = ...
Connection conn = ds.getConnection();
Statement stmt1 = conn.createStatement();
Statement stmt2 = conn.createStatement();
ResultSet rs = stmt1.executeQuery("select * from COURSE");
while (rs.next()) {
  String title = rs.getString("Title");
  boolean goodCourse = getUserDecision(title);
  if (!goodCourse) {
    int id = rs.getInt("CId");
    stmt2.executeUpdate("delete from COURSE where CId =" + id);
  }
}
rs.close();
```

このコードは、ユーザが COURSE テーブルをイテレートし、必要なレコードを削除することを許可していた。以下では、そのコードを更新可能な ResultSet を使用するように修正している。削除された行は、next を呼び出すまで現在のままである。
```java
DataSource ds = ...
Connection conn = ds.getConnection();
conn.setAutocommit(false);
Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
ResultSet rs = stmt.executeQuery("select * from COURSE");
while (rs.next()) {
  String title = rs.getString("Title");
  boolean goodCourse = getUserDecision(title);
  if (!goodCourse) {
    rs.deleteRow();
  }
}
rs.close();
stmt.close();
conn.commit();
```

スクロール可能な ResultSet は、ほとんどの場合、クライアントが出力レコードに対して何を行うかを知っており、2度 目にそれらを調べる必要がないため、限られた用途しかない。クライアントは通常、クエリの結果を表示するクライアントでのみスクロール可能な ResultSet が必要である。例えば、クエリの出力を Swing の JTable オブジェクトとして表示したいクライアントを考えてみる。JTable は、画面に収まらないほど多くの出力レコードがある場合にスクロールバーを表示し、ユーザがスクロールバーをクリックしてレコードを前後に移動できるようにする。この状況では、クライアントが JTable オブジェクトにスクロール可能な ResultSet を提供する必要がある。これにより、ユーザがスクロールバックすると以前のレコードを取得できる。

### 2.2.6 追加のデータ型

整数や文字列の値に加えて、JDBC には多数の他の型を操作するためのメソッドも含まれている。たとえば ResultSet インターフェースを考えてみると、getInt や getString メソッドに加えて、getFloat, getDouble, getShort, getTime, getDateなどのメソッドもある。これらのメソッドのそれぞれは、現在のレコードの指定されたフィールドから値を読み取り、（可能であれば）指定された Java の型に変換する。一般的には、数値の SQL フィールドには数値の JDBC メソッド（getInt、getFloatなど）を使用することが最も意味がある。しかし JDBC は、メソッドで示された Java の型に任意の SQL 値を変換しようとする。特に、任意の SQL 値を Java の文字列に変換することは常に可能である。

## 2.3 Java と SQL での計算の比較

プログラマーが JDBC クライアントを書く際に重要な決定をしなければならない場面がある。それは、どの計算をデータベースエンジンが行い、どの計算を Java クライアントが行うかということである。このセクションでは、これらの問いについて検討する。

以下の StudentMajor デモクライアントを再考する。
```java
public class StudentMajor {
  public static void main(String[] args) {
    String url = "jdbc:derby://localhost/studentdb";
    String qry = "select SName, DName from DEPT, STUDENT " + "where MajorId = DId";
    Driver d = new ClientDriver();
    try (Connection conn = d.connect(url, null);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(qry)) {
      System.out.println("Name\tMajor");
      while (rs.next()) {
        String sname = rs.getString("SName");
        String dname = rs.getString("DName");
        System.out.println(sname + "\t" + dname);
      }
    } catch(SQLException e) {
      e.printStackTrace();
    }
  }
}
```

このプログラムでは、エンジンは STUDENT と DEPT テーブルの結合を計算するために SQL クエリを実行する。クライアントの唯一の責任は、クエリの出力を取得して表示することである。

対照的に、クライアントがすべての計算を行うようにしてもよい。そのコードを以下に示す。
```java
public class BadStudentMajor {
  public static void main(String[] args) {
    ClientDataSource ds = new ClientDataSource();
    ds.setServerName("localhost");
    ds.setDatabaseName("studentdb");
    Connection conn = null;
    try {
      conn = ds.getConnection();
      conn.setAutoCommit(false);
      try (Statement stmt1 = conn.createStatement();
           Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
           ResultSet rs1 = stmt1.executeQuery("select * from STUDENT");
           ResultSet rs2 = stmt2.executeQuery("select * from DEPT")) {
        System.out.println("Name\tMajor");
        while (rs1.next()) {
          // get the next student
          String sname = rs1.getString("SName");
          String dname = null;
          rs2.beforeFirst();
          while (rs2.next()) {
            // search for the major department of that student
            if (rs2.getInt("DId") == rs1.getInt("MajorId")) {
              dname = rs2.getString("DName");
              break;
            }
          }
          System.out.println(sname + "\t" + dname);
        }
      }
      conn.commit();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
      try {
        if (conn != null) {
          conn.rollback();
          conn.close();
        }
      } catch (SQLException e2) {}
    }
  }
}
```

このコードでは、エンジンの唯一の責任は、STUDENT と DEPT テーブルのための ResultSet を作成することである。クライアントは残りの作業をすべて行い、結合を計算して結果を表示する。

これらの 2 つのバージョンのうち、どちらがより良いのだろうか？明らかに、元のバージョンの方がより優雅である。コード量が少なく、読みやすいからである。しかし、効率はどうだろうか？経験則として、クライアントでできるだけ少ないことを行う方が常に効率的である。その理由は2つある。

- エンジンからクライアントへのデータ転送が少なくてすむため、特に異なるマシン上にある場合に重要である。
- エンジンには、各テーブルがどのように実装されており、複雑なクエリ（結合など）を計算する可能な方法についての詳細な専門知識が含まれている。クライアントがエンジンと同じ効率でクエリを計算することはほとんどありえない。

たとえば、先ほどのコードは、2 つの入れ子ループを使用して結合を計算する。外側のループは STUDENT レコードを反復処理する。各学生に対して、内側のループはその学生の専攻に一致する DEPT レコードを検索する。これは合理的な結合アルゴリズムだが、特に効率的とは言えない。Chapter 13, 14 では、はるかに効率的な実行を導くいくつかの技術が議論されている。

先程の 2 つのコードは、本当に良い JDBC コードと本当に悪い JDBC コードの極端な例を示しており、その比較はかなり容易だった。しかし、時には、比較が難しいこともある。たとえば、再び先程の PreparedFindMajors デモクライアントを考えてみる。
```java
public class PreparedFindMajors {
  public static void main(String[] args) {
    System.out.print("Enter a department name: ");
    Scanner sc = new Scanner(System.in);
    String major = sc.next();
    sc.close();
    String qry = "select sname, gradyear from student, dept " + "where did = majorid and dname = ?";
    ClientDataSource ds = new ClientDataSource();
    ds.setServerName("localhost");
    ds.setDatabaseName("studentdb");
    try (Connection conn = ds.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(qry)) {
      pstmt.setString(1, major);
      ResultSet rs = pstmt.executeQuery();
      System.out.println("Here are the " + major + " majors");
      System.out.println("Name\tGradYear");
      while (rs.next()) {
        String sname = rs.getString("sname");
        int gradyear = rs.getInt("gradyear");
        System.out.println(sname + "\t" + gradyear);
      }
      rs.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
```

このコードは、指定された専攻の学生を返す。このコードはエンジンに、STUDENT と MAJOR を join した SQL クエリを実行するように依頼する。join を実行するのに時間がかかる可能性があることがわかっている。真剣な考慮の結果、join を使用せずに必要なデータを取得できることがわかった。アイデアは、2 つのシングルテーブルクエリを使用することである。最初のクエリは、指定された専攻名を持つレコードを探し、その DId の値を返す。次のクエリは、その値を使用して STUDENT レコードの MajorID の値を検索する。このアルゴリズムのコードを以下に示す。このアルゴリズムはシンプルで優雅で効率的である。2 つのテーブルを順次スキャンするだけで、join よりもはるかに高速であるはずである。自分の努力に満足できるだろう。
```java
public class CleverFindMajors {
  public static void main(String[] args) {
    String major = args[0];
    String qry1 = "select DId from DEPT where DName = ?";
    String qry2 = "select * from STUDENT where MajorId = ?";

    ClientDataSource ds = new ClientDataSource();
    ds.setServerName("localhost");
    ds.setDatabaseName("studentdb");
    try (Connection conn = ds.getConnection()) {
      PreparedStatement stmt1 = conn.prepareStatement(qry1);
      stmt1.setString(1, major);
      ResultSet rs1 = stmt1.executeQuery();
      rs1.next();
      rs1.close();
      stmt1.close();
      
      PreparedStatement stmt2 = conn.prepareStatement(qry2);
      stmt2.setInt(1, deptid);
      ResultSet rs2 = stmt2.executeQuery();
      System.out.println("Here are the " + major + " majors");
      System.out.println("Name\tGradYear");
      while (rs2.next()) {
        String sname = rs2.getString("sname");
        int gradyear = rs2.getInt("gradyear");
        System.out.println(sname + "\t" + gradyear);
      }
      rs2.close();
      stmt2.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
```

残念ながら、あなたの努力は無駄に終わるだろう。新しいアルゴリズムは実際には新しいものではなく、Chap. 14 のマルチバッファプロダクトとマテリアライズされた内部テーブルの巧妙な実装である。うまく書かれたデータベースエンジンは、このアルゴリズム（および他のいくつかのアルゴリズム）について知っており、それが最も効率的であるとわかった場合にはそれを使用して join を計算する。すべての賢さはデータベースエンジンによって先取りされていた。教訓は、StudentMajor クライアントと同じで、エンジンに仕事をさせることが最も効率的な戦略である（およびコーディングが最も簡単な戦略であること）ということだ。

初心者の JDBC プログラマーがする間違いの一つは、クライアントで行うことをしすぎることである。プログラマーは、Java でクエリを実装するための非常に賢い方法を知っていると思うかもしれない。または、クエリを SQL でどのように表現すればよいかわからず、クエリを Java でコーディングする方が安心感があるかもしれない。これらの場合、クエリを Java でコーディングする決定はほぼ常に誤っている。プログラマーは、データベースエンジンがその仕事をすることを信頼する必要がある。

## 2.4 サマリ

- JDBC メソッドは、Java クライアントとデータベースエンジン間のデータ転送を管理する。
- 基本的な JDBC には、Driver, Connection, Statement, ResultSet, ResultSetMetaData の 5 つのインターフェースがある。
- Driver オブジェクトは、エンジンへの接続の低レベルの詳細をカプセル化する。クライアントがエンジンに接続する場合は、適切なドライバークラスのコピーを取得する必要がある。ドライバークラスとその接続文字列は、JDBC プログラムのベンダー固有のコードであり、それ以外はベンダー中立の JDBC インターフェースを参照する。
- リソースを保持する ResultSet と Connection は、他のクライアントが必要とするかもしれない。JDBC クライアントは、できるだけ早くそれらを閉じるべきである。
- すべての JDBC メソッドは SQLException をスローする可能性がある。クライアントはこれらの例外をチェックする義務がある。
- ResultSetMetaData クラスのメソッドは、出力テーブルのスキーマに関する情報を提供する。つまり、各フィールドの名前、タイプ、および表示サイズである。この情報は、クライアントがユーザーから直接クエリを受け入れる場合、たとえば SQL インタプリターの場合に役立つ。
- 基本的な JDBC クライアントは、Driver クラスを直接呼び出す。フル JDBC は、コネクションプロセスを簡素化し、ベンダー中立にするために、クライアントに DriverManager クラスと DataSource インターフェースを提供する。
- DriverManager クラスは、ドライバーのコレクションを保持する。クライアントは、ドライバーマネージャーにドライバーを明示的に登録するか、システムプロパティファイルを介して登録する。クライアントがデータベースに接続したい場合、接続文字列を DriverManagaer に提供し、クライアントのために接続を行う。
- DataSource オブジェクトは、ドライバーと接続文字列の両方をカプセル化し、さらにはベンダー中立である。したがって、クライアントは Connection の詳細を知らなくても、データベースエンジンに接続できる。データベース管理者は、さまざまな DataSource オブジェクトを作成し、クライアントが使用できるようにサーバーに配置できる。
- 基本的な JDBC クライアントは、トランザクションの存在を無視する。データベースエンジンはこれらのクライアントをオートコミットモードで実行する。これは、各 SQL ステートメントが独自のトランザクションであることを意味する。
- トランザクション内のすべてのデータベース相互作用は、ユニットとして扱われる。トランザクションは、現在の作業ユニットが正常に完了したときにコミットされる。トランザクションがコミットできない場合は、ロールバックされる。データベースエンジンは、ロールバックを実装することで、そのトランザクションによって行われたすべての変更を元に戻す。
- 自動コミットは、シンプルで重要でない JDBC クライアントにとって合理的なデフォルトモードである。クライアントが重要なタスクを実行する場合、そのプログラマーはトランザクションのニーズを慎重に分析する必要がある。クライアントは、`setAutoCommit(false)` メソッドを呼び出すことでオートコミットをオフにする。この呼び出しにより、エンジンが新しいトランザクションを開始する。その後、クライアントは、現在のトランザクションを完了し、新しいトランザクションを開始する必要があるときに、commit または rollback を呼び出す。クライアントが自動コミットをオフにすると、関連するトランザクションが失敗した SQL ステートメントを処理する必要がある。
- クライアントは、setTransactionIsolation メソッドを使用して、その分離レベルを指定することもできる。JDBC は、4 つの分離レベルを定義している。
  - Read-Uncommitted 分離は、まったく分離されていない。トランザクションは、未コミットのデータ、繰り返しでない読み取り、またはファントムレコードの読み取りに起因する問題が発生する可能性がある。
  - Read-Committed 分離は、トランザクションが未コミットの値にアクセスするのを禁止する。繰り返しでない読み取りやファントムレコードに関連する問題は、引き続き発生する可能性がある。
  - Repeatable-Read 分離は、常に読み取りが繰り返されるように read-committed を拡張する。発生する可能性のある問題は、ファントムレコードのみである。
  - Serializable 分離は、問題が発生しないことを保証する。
- Serializable 分離は明らかに好ましいが、その実装によってトランザクションが遅くなる傾向がある。プログラマーは、クライアントとの可能な競合エラーのリスクを分析し、そのリスクが許容できる場合にのみ、より制限の少ない分離レベルを選択する必要がある。
- プリペアドステートメントには関連する SQL ステートメントがあり、パラメーターのプレースホルダーを持つことができる。クライアントは後でパラメーターに値を割り当て、その後ステートメントを実行できる。プリペアドステートメントは、動的に生成された SQL ステートメントを処理する便利な方法である。さらに、プリペアドステートメントは、パラメーターが割り当てられる前にコンパイルされるため、複数回（たとえば、ループ内で）プリペアドステートメントを実行すると非常に効率的である。
- フル JDBC では、 ResultSet をスクロール可能かつ更新可能にすることができる。デフォルトでは、レコードセットは前方のみで更新不可である。クライアントがより強力なレコードセットを必要とする場合、`Connection#createStatement` メソッドで指定する。
- JDBC クライアントを作成する際の指針は、エンジンができるだけ多くの作業を行うことである。データベースエンジンは非常に洗練されており、通常は必要なデータを取得する最も効率的な方法を知っている。クライアントが正確に必要なデータを取得する SQL ステートメントを決定し、エンジンに提出することはほとんど常に良いアイデアである。要するに、プログラマーはエンジンが仕事をすることを信頼する必要がある。
