import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Main {

  public static void main(String[] args) throws IOException {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
    final int PORT = 6379;

    try {
      // ── 1  Non-blocking server socket
      ServerSocketChannel server = ServerSocketChannel.open();
      server.bind(new InetSocketAddress(PORT));
      server.configureBlocking(false);

      // ── 2  Selector = event multiplexer
      Selector selector = Selector.open();
      server.register(selector, SelectionKey.OP_ACCEPT);

      System.out.println("Redis-lite (NIO) listening on " + PORT);

      // ── 3  Event loop
      while (true) {
        selector.select();                           // Wait until at least one event
        var iter = selector.selectedKeys().iterator();

        while (iter.hasNext()) {
          SelectionKey key = iter.next();
          iter.remove();                           // Important!

          if (key.isValid() && key.isAcceptable()) {
            // Hand off new client to its own handler object
            SocketChannel client = server.accept();
            new ConnectionHandler(client, selector);
            continue;
          }

          // For READ / WRITE we just delegate to whatever object was attached
          if (key.isValid() && key.attachment() instanceof ConnectionHandler h) {
            h.handleKey(key);                    // read → maybe write
          }
        }
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
