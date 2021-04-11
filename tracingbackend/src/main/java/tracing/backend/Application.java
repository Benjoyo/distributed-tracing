package tracing.backend;

public class Application {

    public static void main(String[] args) {
        new TraceServer().start(args);
    }
}
