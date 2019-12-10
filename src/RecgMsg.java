import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class RecgMsg {
    private String str = "";

    public RecgMsg() {
        str = "{\"no\":4,\"msg\": \"进入识别页面\"}".trim();
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
