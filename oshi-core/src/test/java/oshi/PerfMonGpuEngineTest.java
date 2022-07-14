package oshi;

import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.tuples.Pair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerfMonGpuEngineTest {

    public enum GpuEngineProperty implements PerfCounterWildcardQuery.PdhCounterWildcardProperty {
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCE),
        UTILIZATION("Utilization Percentage"),
        UTILIZATION_BASE("Utilization Percentage_Base");

        private final String counter;

        GpuEngineProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    public static void main(String[] args) throws Exception {
        while (true) {
            Pair<List<String>, Map<GpuEngineProperty, List<Long>>> initial = PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(GpuEngineProperty.class, "GPU Engine");

            Thread.sleep(100);

            Pair<List<String>, Map<GpuEngineProperty, List<Long>>> other = PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(GpuEngineProperty.class, "GPU Engine");

            List<Long> baseA = initial.getB().get(GpuEngineProperty.UTILIZATION_BASE);
            List<Long> baseB = other.getB().get(GpuEngineProperty.UTILIZATION_BASE);
            List<Long> baseDifference = IntStream.range(0, baseA.size())
                .mapToObj(i -> baseB.get(i) - baseA.get(i))
                .collect(Collectors.toList());

            List<Long> utilizationA = initial.getB().get(GpuEngineProperty.UTILIZATION);
            List<Long> utilizationB = other.getB().get(GpuEngineProperty.UTILIZATION);
            List<Double> utilization = IntStream.range(0, utilizationA.size())
                .mapToObj(i -> 100 * (utilizationB.get(i) - utilizationA.get(i)) / (double) baseDifference.get(i))
                .collect(Collectors.toList());

            System.out.println(utilization);
            Thread.sleep(100);

        }
    }
}
