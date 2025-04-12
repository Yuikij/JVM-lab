package org.kubo;

import java.util.ArrayList;
import java.util.Scanner;

import static org.kubo.Utils.triggerGC;

public class MyJvmLab {

    static ArrayList<int[]> temporaryObjects = new ArrayList<>();
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== JVM内存与垃圾回收实验菜单 ===");
            System.out.println("1. 创建临时对象");
            System.out.println("2. 主动触发垃圾回收");
            System.out.println("3. 创建永久对象");
            System.out.println("4. 模拟内存泄漏");
            System.out.println("5. 测试大对象进入老年代");
            System.out.println("6. 测试弱引用");
            System.out.println("7. 退出");

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
//                case 4:
//                    simulateMemoryLeak();
//                    break;
//                case 5:
//                    testLargeObjectToOldGen();
//                    break;
//                case 6:
//                    testWeakReferences();
//                    break;
                case 7:
                    scanner.close();
                    System.out.println("实验结束，感谢使用！");
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


}
