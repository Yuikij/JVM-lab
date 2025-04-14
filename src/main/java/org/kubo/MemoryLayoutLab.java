package org.kubo;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CannotCompileException;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

public class MemoryLayoutLab {

    private static final int _1MB = 1024 * 1024; // 定义 1MB 大小
    private static List<Object> heapLeaker = new ArrayList<>(); // 用于持有堆对象的引用，防止被回收
    private static List<Class<?>> metaspaceLeaker = new ArrayList<>(); // 持有生成的类的引用
    private static int stackDepth = 0; // 记录栈递归深度

    public static void main(String[] args) throws Exception {
        System.out.println("==== JVM 内存布局实验 ====");
        // 输出当前时间（根据之前的用户请求）
        System.out.println("当前时间: " + java.time.LocalDateTime.now());

        // 打印初始内存使用情况
        System.out.println("\n--- 初始内存使用情况 ---");
        printMemoryUsage();

        // 1. 演示堆内存使用
        System.out.println("\n--- 1. 演示堆内存使用 ---");
        demonstrateHeapUsage(50); // 在堆上分配 50MB
        System.out.println(">>> 堆内存分配完成。");
        printMemoryUsage();
        // 可选: 建议进行 GC 并再次打印，观察可能的清理效果
        // System.out.println(">>> 建议进行垃圾回收 (GC)...");
        // System.gc();
        // Thread.sleep(2000); // 给 GC 一点时间
        // printMemoryUsage();
        // heapLeaker.clear(); // 如果希望 GC 后续能回收这部分内存，可以清除引用

        // 2. 演示栈内存使用 (可能导致 StackOverflowError)
        System.out.println("\n--- 2. 演示栈内存使用 ---");
        demonstrateStackUsage();
        // 注意：我们无法在此轻松打印栈的 *大小*，只能观察其限制。

        // 3. 演示元空间使用
        System.out.println("\n--- 3. 演示元空间使用 ---");
        System.out.println(">>> 正在生成和加载类...");
        demonstrateMetaspaceUsage(5000); // 生成并加载 5000 个类
        System.out.println(">>> 元空间使用演示完成。已加载 " + metaspaceLeaker.size() + " 个类。");
        printMemoryUsage();
        // 可选: 建议进行 GC (元空间的清理可能比较复杂或依赖于 GC 类型)
        // System.out.println(">>> 建议对元空间进行垃圾回收 (GC)...");
        // System.gc();
        // Thread.sleep(2000);
        // printMemoryUsage();

        // 4. 解释本地方法栈
        System.out.println("\n--- 4. 本地方法栈 (解释) ---");
        explainNativeMethodStack();

        System.out.println("\n==== 实验结束 ====");
        System.out.println("保持进程运行以便使用外部工具 (如 VisualVM) 进行监控...");
        Thread.sleep(Long.MAX_VALUE); // 无限期休眠，保持进程存活
    }

    /**
     * 在堆上分配对象。
     * @param sizeMB 要分配的总大小 (MB)。
     */
    private static void demonstrateHeapUsage(int sizeMB) {
        System.out.println("尝试在堆上分配约 " + sizeMB + "MB 内存...");
        try {
            for (int i = 0; i < sizeMB; i++) {
                heapLeaker.add(new byte[_1MB]); // 分配 1MB 大小的 byte 数组
                if ((i + 1) % 10 == 0) {
                    System.out.print("."); // 打印进度指示器
                }
            }
            System.out.println("\n堆内存分配循环完成。");
        } catch (OutOfMemoryError e) {
            System.err.println("\n堆内存分配时发生 OutOfMemoryError！已分配约: " + heapLeaker.size() + "MB");
        }
    }

    /**
     * 使用递归消耗栈空间。捕获 StackOverflowError。
     */
    private static void demonstrateStackUsage() {
        System.out.println("尝试深度递归以演示栈内存使用...");
        try {
            stackDepth = 0; // 重置深度计数器
            recursiveCall(); // 开始递归调用
        } catch (StackOverflowError e) {
            System.err.println("捕获到 StackOverflowError！达到的最大递归深度: " + stackDepth);
            // 这演示了当前线程栈的限制。
        }
    }

    // 递归方法
    private static void recursiveCall() {
        stackDepth++; // 增加深度
        recursiveCall(); // 再次调用自身
    }

    /**
     * 动态生成并加载类以消耗元空间。
     * 需要 javassist 库。
     * @param numberOfClassesToGenerate 要生成的唯一类的数量。
     */
    private static void demonstrateMetaspaceUsage(int numberOfClassesToGenerate) {
        System.out.println("尝试生成并加载 " + numberOfClassesToGenerate + " 个类以填充元空间...");
        ClassPool pool = ClassPool.getDefault(); // 获取默认类池
        try {
            for (int i = 0; i < numberOfClassesToGenerate; i++) {
                // 为每次迭代创建唯一的类名
                String className = "org.kubo.gen.MyGeneratedClass" + i;
                CtClass cc = pool.makeClass(className); // 创建类定义

                // 可以按需添加简单方法或字段，但通常仅加载类就足以消耗元空间
                // CtMethod m = CtNewMethod.make("public void hello() { System.out.println(\"来自 "+className+" 的问候\"); }", cc);
                // cc.addMethod(m);

                // 将类加载到 JVM，这会消耗元空间
                Class<?> generatedClass = cc.toClass(MemoryLayoutLab.class.getClassLoader(), null);
                metaspaceLeaker.add(generatedClass); // 保留对加载类的引用，防止被卸载

                // 分离 CtClass 对象以允许 javassist 本身的内存被回收 (可选的内存优化)
                cc.detach();

                if ((i + 1) % 500 == 0) {
                    System.out.print("#"); // 打印进度指示器
                    // 可选: 在加载过程中间歇性打印内存使用情况
                    // printMemoryUsage();
                    // Thread.sleep(100); // 稍微减慢速度
                }
            }
            System.out.println("\n类生成和加载循环完成。");
        } catch (CannotCompileException e) { // 添加了 InterruptedException
            System.err.println("\n元空间演示过程中出错: " + e.getMessage());
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            // 也有可能发生与元空间相关的 OOM (例如, java.lang.OutOfMemoryError: Metaspace)
            System.err.println("\n元空间分配时发生 OutOfMemoryError！已加载 " + metaspaceLeaker.size() + " 个类。");
            e.printStackTrace();
        }
    }


    /**
     * 解释本地方法栈的用途。
     */
    private static void explainNativeMethodStack() {
        System.out.println("本地方法栈用于执行本地（非 Java）代码，通常通过 JNI (Java Native Interface)。");
        System.out.println("其大小通常由操作系统管理，不受标准 JVM 堆/元空间参数的直接控制。");
        System.out.println("要演示显著的使用情况，需要调用那些会消耗其自身栈空间的本地库。");
        System.out.println("这超出了一个简单的纯 Java 实验的范围。");
    }

    /**
     * 使用 JMX Beans 打印详细的内存使用情况。
     */
    private static void printMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean(); // 获取内存管理 Bean
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();     // 获取堆内存使用情况
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage(); // 获取非堆内存使用情况

        System.out.println("--- 内存统计 ---");
        // 堆内存信息
        System.out.printf("堆内存使用:   初始(Init)=%s, 已用(Used)=%s, 已提交(Committed)=%s, 最大(Max)=%s%n",
                formatSize(heapUsage.getInit()),
                formatSize(heapUsage.getUsed()),
                formatSize(heapUsage.getCommitted()),
                formatSize(heapUsage.getMax())
        );
        // 非堆内存信息 (包括元空间, 压缩类空间, 代码缓存等)
        System.out.printf("非堆内存使用: 初始(Init)=%s, 已用(Used)=%s, 已提交(Committed)=%s, 最大(Max)=%s%n",
                formatSize(nonHeapUsage.getInit()),
                formatSize(nonHeapUsage.getUsed()),
                formatSize(nonHeapUsage.getCommitted()),
                formatSize(nonHeapUsage.getMax()) // 最大值可能为 -1 (未定义)
        );

        // 详细的内存池信息 (明确包含元空间)
        System.out.println("内存池详情:");
        List<MemoryPoolMXBean> poolBeans = ManagementFactory.getMemoryPoolMXBeans(); // 获取所有内存池 Bean
        for (MemoryPoolMXBean poolBean : poolBeans) {
            String name = poolBean.getName(); // 内存池名称
            MemoryUsage usage = poolBean.getUsage(); // 该池的使用情况
            System.out.printf("  内存池[%-20s]: 已用(Used)=%s, 已提交(Committed)=%s, 最大(Max)=%s%n",
                    name,
                    formatSize(usage.getUsed()),
                    formatSize(usage.getCommitted()),
                    formatSize(usage.getMax()) // 最大值可能为 -1
            );
            // 特别标识出元空间
            if (name.toLowerCase().contains("metaspace")) {
                System.out.printf("    -> 元空间使用: 已用(Used)=%s%n", formatSize(usage.getUsed()));
            }
        }
        System.out.println("--------------------");
    }

    /**
     * 辅助方法，用于将字节大小格式化为易读的字符串。
     */
    private static String formatSize(long bytes) {
        if (bytes < 0) return "N/A"; // 最大值可能未定义 (-1)
        if (bytes < 1024) return bytes + " B"; // 小于 1KB，直接显示字节
        int exp = (int) (Math.log(bytes) / Math.log(1024)); // 计算数量级
        String pre = "KMGTPE".charAt(exp - 1) + ""; // 获取单位前缀 (K, M, G, ...)
        // 格式化输出，保留一位小数
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public class DirectMemoryCalculator {

        private static final String DIRECT_BUFFER_POOL_NAME = "java.lang:type=BufferPool,name=direct";

        public static void main(String[] args) {
            // 运行一些分配直接内存的代码，以便有东西可以计算
            // (可以复用之前的 DirectMemoryLab 的部分代码来分配一些内存)
            try {
                System.out.println("分配一些直接内存用于演示...");
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(10 * 1024 * 1024); // 分配10MB
                java.nio.ByteBuffer bb2 = java.nio.ByteBuffer.allocateDirect(5 * 1024 * 1024); // 再分配5MB
                System.out.println("分配完成，现在尝试获取直接内存使用量。");

                calculateAndPrintDirectMemoryUsage();

                System.out.println("按 Enter 键退出...");
                System.in.read();
                // bb 和 bb2 在这里仍然被引用，所以内存应该还在

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void calculateAndPrintDirectMemoryUsage() {
            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                ObjectName poolName = new ObjectName(DIRECT_BUFFER_POOL_NAME);

                // 定义要获取的属性名称列表
                String[] attributeNames = {"Count", "MemoryUsed", "TotalCapacity"};
                AttributeList attributes = mbs.getAttributes(poolName, attributeNames);
                List<Attribute> attrList = attributes.asList();

                long memoryUsed = 0;
                long totalCapacity = 0;
                long count = 0;

                // 解析获取到的属性
                for (Attribute attr : attrList) {
                    switch (attr.getName()) {
                        case "MemoryUsed":
                            memoryUsed = (long) attr.getValue();
                            break;
                        case "TotalCapacity":
                            totalCapacity = (long) attr.getValue();
                            break;
                        case "Count":
                            count = (long) attr.getValue();
                            break;
                    }
                }

                System.out.println("--- JMX Direct Buffer Pool 统计 ---");
                System.out.println("Buffer 数量 (Count): " + count);
                System.out.printf("已用内存 (MemoryUsed): %,d 字节 (%.2f MB)%n", memoryUsed, memoryUsed / (1024.0 * 1024.0));
                System.out.printf("总容量 (TotalCapacity): %,d 字节 (%.2f MB)%n", totalCapacity, totalCapacity / (1024.0 * 1024.0));
                System.out.println("----------------------------------");

            } catch (Exception e) {
                System.err.println("无法获取 Direct Buffer Pool MBean 信息: " + e.getMessage());
                // e.printStackTrace(); // 可能 MBean 不存在或没有权限
            }
        }
    }
}