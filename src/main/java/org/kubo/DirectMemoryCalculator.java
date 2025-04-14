package org.kubo;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;

public class DirectMemoryCalculator {

    private static final String DIRECT_BUFFER_POOL_NAME = "java.lang:type=BufferPool,name=direct";

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024 * 100); // 100MB
        List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        for (BufferPoolMXBean pool : pools) {
            if ("direct".equals(pool.getName())) {
                System.out.println("直接内存数量: " + pool.getCount());
                System.out.println("直接内存大小: " + (pool.getMemoryUsed() / 1024 / 1024) + " MB");
            }
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
