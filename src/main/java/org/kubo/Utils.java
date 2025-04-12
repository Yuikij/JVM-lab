package org.kubo;

public class Utils {
    private static final int MB = 1024 * 1024;

    /**
     * 打印当前 JVM 的内存使用情况。
     * 显示最大内存、已分配内存、已使用内存和空闲内存（单位：MB）。
     */
    public static void printMemoryUsage() {
        System.out.println("=== 当前内存使用情况 ===");
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / MB;
        long totalMemory = runtime.totalMemory() / MB;
        long freeMemory = runtime.freeMemory() / MB;
        long usedMemory = totalMemory - freeMemory;

        System.out.println("最大内存: " + maxMemory + " MB");
        System.out.println("已分配内存: " + totalMemory + " MB");
        System.out.println("已使用内存: " + usedMemory + " MB");
        System.out.println("空闲内存: " + freeMemory + " MB");
    }

    /**
     * 主动触发垃圾回收
     */
    public static void triggerGC() {
        System.out.println("主动触发垃圾回收前的内存状态:");
        printMemoryUsage();

        System.out.println("\n执行System.gc()...");
        System.gc();

        System.out.println("\n垃圾回收后的内存状态:");
        printMemoryUsage();
        System.out.println("\n注意：在实际应用中不推荐直接调用System.gc()");
    }
}
