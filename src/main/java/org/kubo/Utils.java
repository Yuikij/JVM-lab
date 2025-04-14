package org.kubo;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

public class Utils {
    public static final int MB = 1024 * 1024;


    /**
     * 获取当前进制内存使用情况
     */
    public static void getTotalProcessMemory() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        int pid = os.getProcessId();
        OSProcess proc = os.getProcess(pid);
        System.out.println("内存占用情况: " + proc.getResidentSetSize() / 1024 / 1024 + " MB");
    }


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

    /**
     * 查看当前元空间使用情况
     */
    public static void printMetaspaceUsage() {

        // 显示当前元空间使用情况
        System.out.println("\n当前元空间使用情况:");
        MemoryPoolMXBean metaspacePool = null;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().contains("Metaspace")) {
                metaspacePool = pool;
                break;
            }
        }
        if (metaspacePool != null) {
            MemoryUsage usage = metaspacePool.getUsage();
            System.out.println("已使用: " + usage.getUsed() / MB + "MB");
            System.out.println("已提交: " + usage.getCommitted() / MB + "MB");
            System.out.println("最大可用: " + (usage.getMax() == -1 ? "无限制" : usage.getMax() / MB + "MB"));
        } else {
            System.out.println("无法获取元空间信息");
        }

    }

    /**
     * 获取已加载的类数
     */
    public static int getLoadedClassCount() {
        int loadedClassCount = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
        System.out.println("\n当前已加载类数量: " + loadedClassCount);
        return loadedClassCount;
    }

    /**
     * 动态生成类
     */

    public static Class<?> createClass(String className, byte[] classData) {
        MyJvmLab.DynamicClassLoader classLoader = new MyJvmLab.DynamicClassLoader();
        return classLoader.defineClass(className, classData);
    }


    /**
     * 动态生成并加载类以消耗元空间。
     * 需要 javassist 库。
     *
     * @param numberOfClassesToGenerate 要生成的唯一类的数量。
     */
    public static void demonstrateMetaspaceUsage(int numberOfClassesToGenerate, List<Class<?>> metaspaceLeaker) {
        System.out.println("尝试生成并加载 " + numberOfClassesToGenerate + " 个类以填充元空间...");
        ClassPool pool = ClassPool.getDefault(); // 获取默认类池
        try {
            int size = metaspaceLeaker.size();
            for (int i = size; i < numberOfClassesToGenerate + size; i++) {
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


}
