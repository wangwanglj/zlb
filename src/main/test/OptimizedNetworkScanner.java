
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class OptimizedNetworkScanner {

    private static final int THREAD_POOL_SIZE = 50;  // 控制线程池大小
    private static final int TIMEOUT_MS = 200;      // 超时时间（ms）

    public static void main(String[] args) throws Exception {
        List<String[]> allIpRanges = new ArrayList<>();
        Queue<String> ipQueue = new ConcurrentLinkedQueue<>();

        // 获取所有可用网段
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                continue; // 过滤回环、虚拟和未启用的网卡
            }

            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                InetAddress inetAddress = address.getAddress();
                if (inetAddress instanceof Inet4Address) {
                    String localIp = inetAddress.getHostAddress();
                    short prefixLength = address.getNetworkPrefixLength();
                    int subnetMask = getSubnetMask(prefixLength);

                    String[] ipRange = getFullIpRange(localIp, subnetMask);
                    System.out.println("检测到网段: " + ipRange[0] + " - " + ipRange[1]);
                    allIpRanges.add(ipRange);
                }
            }
        }

        // 将所有 IP 加入队列
        for (String[] range : allIpRanges) {
            int start = ipToInt(range[0]);
            int end = ipToInt(range[1]);
            for (int ip = start + 1; ip < end; ip++) {
                ipQueue.add(intToIp(ip));
            }
        }

        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();

        // 创建工作线程，让线程池重复利用
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            futures.add(executorService.submit(() -> {
                while (!ipQueue.isEmpty()) {
                    String targetIp = ipQueue.poll();
                    if (targetIp != null) {
                        scanIp(targetIp);
                    }
                }
            }));
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();
    }

    // 扫描 IP
    private static void scanIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isReachable(TIMEOUT_MS)) {
                System.out.println("可访问 IP: " + ip);
            }
        } catch (Exception ignored) {}
    }

    // 计算子网掩码
    private static int getSubnetMask(short prefixLength) {
        return (int) ((-1L << (32 - prefixLength)) & 0xFFFFFFFF);
    }

    // 计算 IP 范围
    private static String[] getFullIpRange(String ip, int subnetMask) {
        int ipInt = ipToInt(ip);
        int networkAddress = ipInt & subnetMask;
        int broadcastAddress = networkAddress | (~subnetMask & 0xFFFFFFFF);

        return new String[]{intToIp(networkAddress), intToIp(broadcastAddress)};
    }

    // IP 转整数
    private static int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        return (Integer.parseInt(parts[0]) << 24) |
                (Integer.parseInt(parts[1]) << 16) |
                (Integer.parseInt(parts[2]) << 8) |
                Integer.parseInt(parts[3]);
    }

    // 整数转 IP
    private static String intToIp(int ip) {
        return ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                (ip & 0xFF);
    }
}
