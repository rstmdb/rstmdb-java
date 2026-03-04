import com.rstmdb.client.RstmdbClientImpl;
import com.rstmdb.client.model.ApplyEventRequest;
import com.rstmdb.client.model.BatchMode;
import com.rstmdb.client.model.BatchOperation;
import com.rstmdb.client.model.CreateInstanceRequest;

import java.util.Map;
import java.util.UUID;

public class BatchExample {
    public static void main(String[] args) throws Exception {
        try (var client = RstmdbClientImpl.connect("localhost", 7401)) {

            // Use unique IDs so the example is re-runnable
            var id1 = "batch-" + UUID.randomUUID().toString().substring(0, 8);
            var id2 = "batch-" + UUID.randomUUID().toString().substring(0, 8);

            // Atomic batch — all or nothing
            var results = client.batchSync(BatchMode.ATOMIC,
                    BatchOperation.createInstance(new CreateInstanceRequest(
                            id1, "order", 1, Map.of("customer", "alice"), null)),
                    BatchOperation.createInstance(new CreateInstanceRequest(
                            id2, "order", 1, Map.of("customer", "bob"), null))
            );

            System.out.println("Atomic batch results:");
            for (var r : results) {
                System.out.println("  " + r.getStatus());
            }

            // Best-effort batch — each op independent
            var results2 = client.batchSync(BatchMode.BEST_EFFORT,
                    BatchOperation.applyEvent(new ApplyEventRequest(
                            id1, "PAY", Map.of("amount", 50.0),
                            null, null, null, null)),
                    BatchOperation.applyEvent(new ApplyEventRequest(
                            id2, "PAY", Map.of("amount", 75.0),
                            null, null, null, null))
            );

            System.out.println("Best-effort batch results:");
            for (var r : results2) {
                System.out.println("  " + r.getStatus());
            }
        }
    }
}
