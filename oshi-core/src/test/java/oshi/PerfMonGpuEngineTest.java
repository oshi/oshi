package oshi;

import oshi.driver.windows.perfmon.GraphicsUtilizationThread;

import java.util.Arrays;

public class PerfMonGpuEngineTest {

    public static void main(String[] args) throws Exception {

        GraphicsUtilizationThread gpuUtilizationThread = new GraphicsUtilizationThread();

        gpuUtilizationThread.start();

        Thread.sleep(1000L);

        System.out.println(Arrays.toString(gpuUtilizationThread.getUtilizationPercentages()));


    }
}
