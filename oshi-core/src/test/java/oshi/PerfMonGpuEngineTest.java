package oshi;

import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.tuples.Pair;

import java.util.List;
import java.util.Map;

public class PerfMonGpuEngineTest {

    public enum GpuEngineProperty implements PerfCounterWildcardQuery.PdhCounterWildcardProperty {
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCE),
        RUNNING_TIME("Running Time"),
        UTILIZATION("Utilization Percentage");

        private final String counter;

        GpuEngineProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    // hmmmmm individual instance dont look right......
    public enum GpuEngineProperty2 implements PerfCounterQuery.PdhCounterProperty {
        UTILIZATION("pid_10472_luid_0x00000000_0x0000f814_phys_0_eng_0_engtype_3d", "Utilization Percentage");

        private final String instance;
        private final String counter;

        GpuEngineProperty2(String instance, String counter) {
            this.counter = counter;
            this.instance = instance;
        }

        @Override
        public String getInstance() {
            return instance;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    public static void main(String[] args) {
//        Pair<List<String>, Map<GpuEngineProperty, List<Long>>> result = PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(GpuEngineProperty.class, "GPU Engine");

//        System.out.println(result.getA());
//        System.out.println(result.getB());
//        System.out.println(result.getA().size());
//        System.out.println(result.getB().get(GpuEngineProperty.UTILIZATION).size());

        System.out.println(PerfCounterQuery.queryValuesFromPDH(GpuEngineProperty2.class, "GPU Engine"));
    }
}
