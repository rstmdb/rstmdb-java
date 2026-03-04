import com.rstmdb.client.RstmdbClientImpl;
import com.rstmdb.client.RstmdbOptions;
import com.rstmdb.client.model.ApplyEventRequest;
import com.rstmdb.client.model.CreateInstanceRequest;
import com.rstmdb.client.model.ListInstancesOptions;
import com.rstmdb.client.model.MachineDefinition;
import com.rstmdb.client.model.PutMachineRequest;
import com.rstmdb.client.model.Transition;

import java.util.List;
import java.util.Map;

public class BasicExample {
    public static void main(String[] args) throws Exception {
        try (var client = RstmdbClientImpl.connect("localhost", 7401,
                RstmdbOptions.builder().build())) {

            // Ping
            client.pingSync();
            System.out.println("Connected to rstmdb");

            // Register a state machine
            client.putMachineSync(new PutMachineRequest(
                    "order", 1,
                    new MachineDefinition(
                            List.of("created", "paid", "shipped", "delivered"),
                            "created",
                            List.of(
                                    new Transition(List.of("created"),
                                            "PAY", "paid", null),
                                    new Transition(List.of("paid"), "SHIP",
                                            "shipped", null),
                                    new Transition(List.of("shipped"),
                                            "DELIVER", "delivered", null)
                            ),
                            null),
                    null));
            System.out.println("Machine 'order' registered");

            // Create an instance
            var instance = client.createInstanceSync(new CreateInstanceRequest(
                    "order-001", "order", 1,
                    Map.of("customer", "alice", "total", 99.99),
                    null));
            System.out.println("Created: " + instance.getInstanceId() + " in state " + instance.getState());

            // Apply event
            var result = client.applyEventSync(new ApplyEventRequest(
                    "order-001", "PAY",
                    Map.of("payment_id", "pay-123"),
                    null, null, null, null));
            System.out.println(result.getFromState() + " -> " + result.getToState());

            // Get instance
            var inst = client.getInstanceSync("order-001");
            System.out.println("Instance state: " + inst.getState());

            // List instances
            var list = client.listInstancesSync(
                    ListInstancesOptions.builder().machine("order").build());
            System.out.println("Total instances: " + list.getTotal());

            // Delete instance
            var deleted = client.deleteInstanceSync("order-001");
            System.out.println("Deleted: " + deleted.isDeleted());
        }
    }
}
