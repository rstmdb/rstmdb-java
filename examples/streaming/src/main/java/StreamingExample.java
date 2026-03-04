import com.rstmdb.client.RstmdbClientImpl;

public class StreamingExample {
    public static void main(String[] args) throws Exception {
        try (var client = RstmdbClientImpl.connect("localhost", 7401)) {

            // Watch all state changes (no filter)
            var sub = client.watchAllSync();

            System.out.println("Listening for all state changes... (Ctrl+C to stop)");
            for (var event : sub.events()) {
                System.out.printf("[%s] %s: %s -> %s (event: %s)%n",
                        event.getMachine(), event.getInstanceId(),
                        event.getFromState(), event.getToState(), event.getEvent());
            }
        }
    }
}
