package org.kubo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DirectMemoryLab {

    // 每次分配的直接内存大小 (例如: 10MB)
    private static final int ALLOCATION_SIZE_BYTES = 10 * 1024 * 1024;
    // 用于持有 ByteBuffer 对象的引用，防止它们被 GC 回收，从而保持直接内存被占用
    private static List<ByteBuffer> directBuffers = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("==== 直接内存增长实验 ====");
        // 获取并打印最大直接内存限制 (注意: 这只是个估计值或默认值，实际限制受 -XX:MaxDirectMemorySize 控制)
        // 使用 ManagementFactory 获取 MaxDirectMemorySize 可能不直接可用或不准确，最好通过命令行参数设置并观察
        // sun.misc.VM.maxDirectMemory() 可以获取，但这是内部 API
        // System.out.println(" अनुमानित अधिकतम प्रत्यक्ष मेमोरी (Configured MaxDirectMemorySize): " + sun.misc.VM.maxDirectMemory() / (1024*1024) + " MB"); // 内部API，不推荐且可能不存在
        System.out.println("请通过 JVM 参数 -XX:MaxDirectMemorySize=... 来设置限制");
        System.out.println("开始持续分配直接内存，每次分配 " + (ALLOCATION_SIZE_BYTES / (1024*1024)) + " MB...");
        System.out.println("请使用操作系统工具 (如 top, htop, Task Manager) 监控此 Java 进程的内存使用情况。");

        long totalAllocated = 0;
        try {
            while (true) {
                // 1. 分配直接内存
                ByteBuffer buffer = ByteBuffer.allocateDirect(ALLOCATION_SIZE_BYTES);

                // 2. 将引用存起来，防止 GC 回收 ByteBuffer 对象
                directBuffers.add(buffer);
                totalAllocated += ALLOCATION_SIZE_BYTES;

                long totalAllocatedMB = totalAllocated / (1024 * 1024);
                System.out.printf("已分配直接内存总量: %d MB (%d buffers)%n",
                        totalAllocatedMB, directBuffers.size());

                // 3. 短暂暂停，方便观察，避免 CPU 占用过高
                try {
                    TimeUnit.MILLISECONDS.sleep(200); // 暂停 200 毫秒
                } catch (InterruptedException e) {
                    System.out.println("线程被中断，停止分配。");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (OutOfMemoryError e) {
            System.err.println("\n!!! OutOfMemoryError 发生 !!!");
            System.err.println("错误信息: " + e.getMessage());
            long totalAllocatedMB = totalAllocated / (1024 * 1024);
            System.err.printf("在错误发生前，已分配的直接内存总量约为: %d MB (%d buffers)%n",
                    totalAllocatedMB, directBuffers.size());
            // 检查错误信息是否明确指向 Direct buffer memory
            if (e.getMessage() != null && e.getMessage().contains("Direct buffer memory")) {
                System.err.println("这很可能是由于达到了 -XX:MaxDirectMemorySize 限制。");
            } else {
                // 也可能是堆内存耗尽导致无法创建 ByteBuffer 对象本身或其他 OOM
                System.err.println("这可能是其他类型的内存溢出，不仅仅是直接内存。检查堆内存 (-Xmx) 设置。");
            }
            // e.printStackTrace(); // 打印详细堆栈
        } finally {
            System.out.println("\n实验结束或因错误中止。");
            System.out.println("进程将保持运行一段时间以便进一步观察...");
            try {
                Thread.sleep(Long.MAX_VALUE); // 保持进程存活
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}