import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class Mp3Msg {
    private String str = "";

    public Mp3Msg(int no) {
        str = "{\"no\":5,\"msg\": \""+no+".mp3\"}".trim();
    }

    public byte[] parse() {
        //Build the byte array according to the server's parsing rules
        byte[] body = str.getBytes(Charset.defaultCharset());
        ByteBuffer bb = ByteBuffer.allocate(4 + body.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(body.length);
        bb.put(body);
        return bb.array();
    }
}
