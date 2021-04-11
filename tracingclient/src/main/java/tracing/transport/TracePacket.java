package tracing.transport;

/**
 * Low-level representation of an event.
 * Sent to the server as JSON.
 */
public class TracePacket {

    public static final transient int TYPE_MEMORY = 0;
    public static final transient int TYPE_FUNCTION = 1;
    public static final transient int TYPE_MESSAGE = 2;
    public static final transient int TYPE_OVERFLOW = 3;
    public static final transient int TYPE_LOG = 4;

    public static final transient int SUBTYPE_WRITE = 0;
    public static final transient int SUBTYPE_READ = 1;
    public static final transient int SUBTYPE_ENTER = 0;
    public static final transient int SUBTYPE_EXIT = 1;
    public static final transient int SUBTYPE_SEND = 0;
    public static final transient int SUBTYPE_RECEIVE = 1;

    // packet type
    private Integer i;
    // packet sub-type
    private Integer j;

    // called function address
    private Long f;
    // call site address
    private Long c;

    // memory address
    private Long a;
    // memory value
    private Long v;

    // message id
    private String m;

    // log message
    private String l;

    // timestamp
    private Long t;

    public TracePacket() {}

    public Integer getI() {
        return i;
    }

    public void setType(Integer i) {
        this.i = i;
    }

    public Integer getJ() {
        return j;
    }

    public void setSubType(Integer j) {
        this.j = j;
    }

    public Long getF() {
        return f;
    }

    public void setFunctionAddress(Long f) {
        this.f = f;
    }

    public Long getC() {
        return c;
    }

    public void setCallSiteAddress(Long c) {
        this.c = c;
    }

    public Long getA() {
        return a;
    }

    public void setMemoryAddress(Long a) {
        this.a = a;
    }

    public Long getV() {
        return v;
    }

    public void setMemoryValue(Long v) {
        this.v = v;
    }

    public Long getT() {
        return t;
    }

    public void setTimestamp(Long t) {
        this.t = t;
    }

    public String getM() {
        return m;
    }

    public void setMessageId(String m) {
        this.m = m;
    }

    public void setLogMessage(String l) {
        this.l = l;
    }

    public String getL() {
        return l;
    }

    @Override
    public String toString() {
        return "TracePacket{" +
                "i=" + i +
                ", j=" + j +
                ", f=" + f +
                ", c=" + c +
                ", a=" + a +
                ", v=" + v +
                ", m='" + m + '\'' +
                ", l='" + l + '\'' +
                ", t=" + t +
                '}';
    }
}
