package oshi.driver.windows.perfmon;

import oshi.util.tuples.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Utility to calculate graphics utilization via caching intervals of every
 * 100ms
 */
public class GraphicsUtilizationThread extends Thread {

    /**
     * Gpu name corresponding to each engine
     */
    private List<String> gpuNames;

    /**
     * Old utilization values
     */
    private List<Long> utilizations;

    /**
     * Old utilization base
     */
    private List<Long> utilizationBases;

    /**
     * Calculated utilization percentages
     */
    private double[] utilizationPercentages;

    /**
     * Constructor
     */
    public GraphicsUtilizationThread() {
        setName("OSHI Graphics Utilization daemon");
        setDaemon(true);
    }

    /**
     * Start the thread to compute GPU utilization
     */
    @Override
    public void run() {

        // setup initial utilization values
        Pair<List<String>, Map<ProcessInformation.GpuEngineProperty, List<Long>>> initialCounter = ProcessInformation
                .queryGpuCounters();
        Map<ProcessInformation.GpuEngineProperty, List<Long>> oldGpuEngineProperties = initialCounter.getB();

        gpuNames = initialCounter.getA();
        utilizationBases = oldGpuEngineProperties.get(ProcessInformation.GpuEngineProperty.UTILIZATION_BASE);
        utilizations = oldGpuEngineProperties.get(ProcessInformation.GpuEngineProperty.UTILIZATION);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            Pair<List<String>, Map<ProcessInformation.GpuEngineProperty, List<Long>>> newCounters = ProcessInformation
                    .queryGpuCounters();
            Map<ProcessInformation.GpuEngineProperty, List<Long>> newGpuEngineProperties = newCounters.getB();

            List<Long> newUtilizationBases = newGpuEngineProperties
                    .get(ProcessInformation.GpuEngineProperty.UTILIZATION_BASE);
            List<Long> newUtilizations = newGpuEngineProperties.get(ProcessInformation.GpuEngineProperty.UTILIZATION);

            // calculate utilization percentage via the difference between last 100ms and
            // current utilization over the difference between their bases
            utilizationPercentages = IntStream.range(0, gpuNames.size())
                    .mapToDouble(i -> 100 * (newUtilizations.get(i) - utilizations.get(i))
                            / (double) (newUtilizationBases.get(i) - utilizationBases.get(i)))
                    .toArray();

            // update old utilization counters to perform next interval computations
            utilizationBases = newUtilizationBases;
            utilizations = newUtilizations;
        }
    }

    /**
     * Gpu Utilization percentage calculated over each 100ms interval
     *
     * @return utilization percentages
     */
    public double[] getUtilizationPercentages() {
        return utilizationPercentages;
    }

    /**
     * Gpu name array corresponding to each individual engine
     *
     * @return gpu names as list of strings
     */
    public List<String> getGpuNames() {
        return gpuNames;
    }
}
