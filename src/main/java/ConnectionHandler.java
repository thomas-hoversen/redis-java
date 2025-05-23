import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

class ConnectionHandler {

  private static final ByteBuffer PONG =
      ByteBuffer.wrap("+PONG\r\n".getBytes());

  private final SocketChannel channel;
  private final ByteBuffer in = ByteBuffer.allocate(256);

  ConnectionHandler(SocketChannel channel, Selector selector) throws IOException {
    this.channel = channel;
    channel.configureBlocking(false);

    // Register interest in READ events and attach *this* instance to the key
    channel.register(selector, SelectionKey.OP_READ, this);
  }

  /**
   * Central entry-point from the event loop
   */
  void handleKey(SelectionKey key) throws IOException {
    if (key.isReadable()) {
      handleRead(key);
    }
    // You could also check key.isWritable() and flush queued buffers here.
  }

  private void handleRead(SelectionKey key) throws IOException {
    in.clear();
    int n = channel.read(in);
    if (n == -1) {                 // client closed → cancel key & close channel
      key.cancel();
      channel.close();
      return;
    }
    in.flip();
    String req = StandardCharsets.UTF_8.decode(in).toString().trim();
    System.out.println("→ " + req);

    if ("PING".equals(req.substring(req.length()-4))) { // just grab last 4 bytes for now
      channel.write(PONG.duplicate()); // duplicate so position/limit reset
    }
    // For bigger replies you’d queue a buffer and enable OP_WRITE.
  }
}
