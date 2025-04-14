package org.kubo;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;

import static org.kubo.Utils.*;

public class MyJvmLab {

    static ArrayList<int[]> temporaryObjects = new ArrayList<>();
    private static Map<String, String> metaspaceObjects = new HashMap<>();
    private static List<Class<?>> metaspaceLeaker = new ArrayList<>(); // 持有生成的类的引用

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== JVM内存与垃圾回收实验菜单 ===");
            System.out.println("1. 创建临时对象");
            System.out.println("2. 主动触发垃圾回收");
            System.out.println("3. 创建永久对象");
            System.out.println("4. 测试元空间");
            System.out.println("5. 打印元空间");
            System.out.println("6. 添加动态类50个");
            System.out.println("7. 获取已加载的类数量");
            System.out.println("8. 获取当前进制内存使用情况");
            System.out.println("0. 退出");

            System.out.print("请选择操作: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    createTemporaryObject();
                    Utils.printMemoryUsage();
                    break;
                case 2:
                    Utils.triggerGC();
                    break;
                case 3:
                    temporaryObjects.add(createPermanentObject());
                    break;
                case 4:
                    testMetaspace();
                    break;
                case 5:
                    printMetaspaceUsage();
                    break;
                case 6:
                    demonstrateMetaspaceUsage(5000, metaspaceLeaker);
                    break;
                case 7:
                    getLoadedClassCount();
                    break;
                case 8:
                    getTotalProcessMemory();
                    break;
                case 0:
                    System.out.println("实验结束，感谢使用！");
                    scanner.close();
                    return;
                default:
                    System.out.println("无效选择，请重试");
            }
        }
    }

    private static int[] createPermanentObject() {
        Utils.printMemoryUsage();
        int[] largeArray = new int[10000000];
        Utils.printMemoryUsage();
        return largeArray;
    }

    private static void createTemporaryObject() {
        Utils.printMemoryUsage();
        int[] largeArray = new int[10000000];
        Utils.printMemoryUsage();
    }

    static class DynamicClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    /**
     * 测试方法区/元空间
     */
    private static void testMetaspace() {
        System.out.println("\n===== 方法区/元空间测试 =====");
        System.out.println("方法区在JDK8之前是堆的一部分(称为PermGen)，JDK8后改为本地内存中的元空间(Metaspace)");
        System.out.println("方法区/元空间存储类信息、常量、静态变量等");

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

        // 获取已加载的类数
        ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
        System.out.println("\n当前已加载类数量: " + classLoadingBean.getLoadedClassCount());

        // 模拟填充元空间
        Scanner scanner = new Scanner(System.in);
        System.out.print("\n是否测试动态生成类来填充元空间? (y/n): ");
        String response = scanner.nextLine();

        if (response.equalsIgnoreCase("y")) {
            System.out.print("请输入要生成的类数量: ");
            int classCount = scanner.nextInt();

            System.out.println("\n开始生成类...");
            DynamicClassLoader classLoader = new DynamicClassLoader();

            try {
                for (int i = 0; i < classCount; i++) {
                    String className = "org.kubo.DynamicClass" + i;
                    Class<?> clazz = classLoader.defineClass(className, generateClassBytes(className));

                    // 强制初始化类
                    Class.forName(className, true, classLoader);

                    if (i % 1000 == 0) {
                        System.out.println("已生成 " + i + " 个类");

                        // 打印当前元空间使用情况
                        if (metaspacePool != null) {
                            MemoryUsage usage = metaspacePool.getUsage();
                            System.out.println("元空间已使用: " + usage.getUsed() / (1024 * 1024) + "MB");
                        }
                    }
                }
            } catch (OutOfMemoryError e) {
                System.out.println("元空间内存溢出: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("生成类时出错: " + e.getMessage());
                e.printStackTrace();
            }

            // 显示测试后的元空间使用情况
            System.out.println("\n测试后元空间使用情况:");
            if (metaspacePool != null) {
                MemoryUsage usage = metaspacePool.getUsage();
                System.out.println("已使用: " + usage.getUsed() / MB + "MB");
                System.out.println("已提交: " + usage.getCommitted() / MB + "MB");
            }

            System.out.println("当前已加载类数量: " + classLoadingBean.getLoadedClassCount());
        }

        // 测试常量池
        testStringConstantPool();
    }

    // 生成简单类的字节码
    private static byte[] generateClassBytes(String className) {
        // 这里仅模拟生成字节码，实际上需要ASM或Javassist等库来真正生成
        // 在这个简化实现中，我们返回一个空数组
        return new byte[1024]; // 假设每个类占用1KB
    }

    private static void testStringConstantPool() {
        System.out.println("\n-- 字符串常量池测试 --");

        // 清理之前的测试数据
        metaspaceObjects.clear();
        System.gc();

        // 获取测试前内存使用情况
        MemoryUsage heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        System.out.println("向常量池中添加大量字符串...");
        int count = 100000;
        for (int i = 0; i < count; i++) {
            // intern()方法会将字符串放入常量池
            String constantString = ("ConstantString" + i).intern();
            metaspaceObjects.put("key" + i, constantString);

            if (i % 10000 == 0) {
                System.out.println("已添加 " + i + " 个字符串");
            }
        }

        // 获取测试后内存使用情况
        MemoryUsage heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        System.out.println("\n添加 " + count + " 个字符串后内存变化:");
        System.out.println("堆内存使用增加: " + (heapAfter.getUsed() - heapBefore.getUsed()) / 1024 + "KB");
        System.out.println("每个intern字符串平均占用: " + ((heapAfter.getUsed() - heapBefore.getUsed()) / count) + " 字节");
    }

}
