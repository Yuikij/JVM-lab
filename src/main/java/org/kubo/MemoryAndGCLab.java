package org.kubo;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * JVM垃圾回收与内存分布实验类
 */
public class MemoryAndGCLab {

    private static final int MB = 1024 * 1024;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 显示JVM参数信息
        printJVMInfo();

        while (true) {
            System.out.println("\n=== JVM内存与垃圾回收实验菜单 ===");
            System.out.println("1. 查看当前内存使用情况");
            System.out.println("2. 主动触发垃圾回收");
            System.out.println("3. 创建大量对象（模拟内存压力）");
            System.out.println("4. 模拟内存泄漏");
            System.out.println("5. 测试大对象进入老年代");
            System.out.println("6. 测试弱引用");
            System.out.println("7. 退出");

            System.out.print("请选择操作: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    printMemoryUsage();
                    break;
                case 2:
                    triggerGC();
                    break;
                case 3:
                    createManyObjects();
                    break;
                case 4:
                    simulateMemoryLeak();
                    break;
                case 5:
                    testLargeObjectToOldGen();
                    break;
                case 6:
                    testWeakReferences();
                    break;
                case 7:
                    scanner.close();
                    System.out.println("实验结束，感谢使用！");
                    return;
                default:
                    System.out.println("无效选择，请重试");
            }
        }
    }

    /**
     * 显示JVM参数信息
     */
    private static void printJVMInfo() {
        System.out.println("=== JVM信息 ===");
        System.out.println("Java版本: " + System.getProperty("java.version"));
        System.out.println("JVM名称: " + System.getProperty("java.vm.name"));
        System.out.println("JVM版本: " + System.getProperty("java.vm.version"));
        System.out.println("最大内存: " + Runtime.getRuntime().maxMemory() / MB + " MB");
        System.out.println("总内存: " + Runtime.getRuntime().totalMemory() / MB + " MB");
        System.out.println("空闲内存: " + Runtime.getRuntime().freeMemory() / MB + " MB");

        System.out.println("\n=== 建议的JVM参数 ===");
        System.out.println("-Xms128m -Xmx512m -XX:+PrintGCDetails -XX:+UseSerialGC");
        System.out.println("可以通过在运行时添加这些参数来监控GC情况");
    }

    /**
     * 打印当前内存使用情况
     */
    private static void printMemoryUsage() {
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
    private static void triggerGC() {
        System.out.println("主动触发垃圾回收前的内存状态:");
        printMemoryUsage();

        System.out.println("\n执行System.gc()...");
        System.gc();

        System.out.println("\n垃圾回收后的内存状态:");
        printMemoryUsage();
        System.out.println("\n注意：在实际应用中不推荐直接调用System.gc()");
    }

    /**
     * 创建大量对象，模拟内存压力
     */
    private static void createManyObjects() {
        System.out.println("创建大量对象前的内存状态:");
        printMemoryUsage();

        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            // 每个对象约1MB
            byte[] bytes = new byte[MB];
            list.add(bytes);
            System.out.println("已创建 " + (i + 1) + " 个对象，每个约1MB");

            // 每创建10个对象打印一次内存状态
            if ((i + 1) % 10 == 0) {
                printMemoryUsage();
            }

            try {
                Thread.sleep(100); // 暂停一下，便于观察
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n所有对象创建完毕，现在释放引用");
        list.clear();
        System.gc();

        System.out.println("\n释放后的内存状态:");
        printMemoryUsage();
    }

    // 用于模拟内存泄漏的静态集合
    private static List<Object> leakyList = new ArrayList<>();

    /**
     * 模拟内存泄漏
     */
    private static void simulateMemoryLeak() {
        System.out.println("模拟内存泄漏前的内存状态:");
        printMemoryUsage();

        System.out.println("\n向静态集合中添加对象...");
        for (int i = 0; i < 50; i++) {
            // 每个对象约1MB
            leakyList.add(new byte[MB]);
            System.out.println("已向静态集合添加 " + (i + 1) + " 个对象，每个约1MB");

            // 每添加10个对象打印一次内存状态
            if ((i + 1) % 10 == 0) {
                printMemoryUsage();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n对象添加完毕。由于引用仍然存在于静态集合中，这些对象不会被垃圾回收");
        System.out.println("执行垃圾回收...");
        System.gc();

        System.out.println("\n垃圾回收后的内存状态:");
        printMemoryUsage();
    }

    /**
     * 测试大对象直接进入老年代
     */
    private static void testLargeObjectToOldGen() {
        System.out.println("测试大对象直接进入老年代（需开启-XX:+PrintGCDetails参数才能观察到详情）");
        printMemoryUsage();

        System.out.println("\n创建一个20MB的大对象...");
        byte[] largeObject = new byte[20 * MB];

        System.out.println("大对象创建完毕，触发GC...");
        System.gc();

        System.out.println("\n垃圾回收后的内存状态:");
        printMemoryUsage();

        // 保持引用一段时间，便于观察
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 释放引用
        largeObject = null;
        System.gc();

        System.out.println("\n释放大对象后的内存状态:");
        printMemoryUsage();
    }

    /**
     * 测试弱引用
     */
    private static void testWeakReferences() {
        System.out.println("测试弱引用...");

        // 创建一个弱引用对象
        Object referent = new byte[10 * MB];
        java.lang.ref.WeakReference<Object> weakRef = new java.lang.ref.WeakReference<>(referent);

        System.out.println("创建了一个10MB对象，并为其创建了弱引用");
        printMemoryUsage();

        System.out.println("\n弱引用对象是否可达: " + (weakRef.get() != null));

        System.out.println("现在，将强引用设为null");
        referent = null;

        System.out.println("触发垃圾回收...");
        System.gc();

        System.out.println("\n垃圾回收后，弱引用对象是否可达: " + (weakRef.get() != null));
        System.out.println("注意：弱引用在GC时会被回收，即使内存充足");

        printMemoryUsage();
    }
}