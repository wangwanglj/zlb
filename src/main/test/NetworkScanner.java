import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NetworkScanner {

    public static void main(String[] args) throws Exception {
        // 获取本机的 IP 地址
        InetAddress localHost = InetAddress.getLocalHost();
        String localIp = localHost.getHostAddress();
        System.out.println("本机 IP: " + localIp);

        // 获取网络接口信息
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
        if (networkInterface == null) {
            System.out.println("无法获取网络接口信息");
            return;
        }

        // 获取子网掩码的网络前缀长度
        short prefixLength = networkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
        System.out.println("子网前缀长度: " + prefixLength);

        // 计算子网掩码
        int subnetMask = getSubnetMask(prefixLength);
        System.out.println("子网掩码 (整数表示): " + Integer.toBinaryString(subnetMask));

        // 获取本机 IP 地址并计算网段
        String subnet = calculateSubnet(localIp, subnetMask);
        System.out.println("计算得到的网段: " + subnet);

        // 获取网段的开始和结束 IP 地址
        String[] range = getIpRange(subnet, subnetMask);
        System.out.println("IP 范围: " + range[0] + " 到 " + range[1]);

        // 执行扫描
        ExecutorService executorService = Executors.newFixedThreadPool(256);
        List<Future<Boolean>> futures = new ArrayList<>();

        // 扫描网段内所有 IP 地址（排除网络地址和广播地址）
        String startIp = range[0];
        String endIp = range[1];
        String[] startParts = startIp.split("\\.");
        String[] endParts = endIp.split("\\.");
        int startLastOctet = Integer.parseInt(startParts[3]);
        int endLastOctet = Integer.parseInt(endParts[3]);

        for (int i = startLastOctet + 1; i < endLastOctet; i++) { // 避免扫描网络地址和广播地址
            final String targetIp = subnet + "." + i;
            futures.add(executorService.submit(() -> {
                try {
                    InetAddress addressToCheck = InetAddress.getByName(targetIp);
                    if (addressToCheck.isReachable(1000)) { // 1秒超时
                        System.out.println("可访问 IP: " + targetIp);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        // 等待所有任务完成
        for (Future<Boolean> future : futures) {
            future.get();
        }

        executorService.shutdown();
    }

    // 根据前缀长度计算子网掩码
    private static int getSubnetMask(short prefixLength) {
        return (int) ((-1L << (32 - prefixLength)) & 0xFFFFFFFF);
    }

    // 根据子网掩码和本机 IP 计算出网段
    private static String calculateSubnet(String ip, int subnetMask) {
        String[] ipParts = ip.split("\\.");
        StringBuilder subnetBuilder = new StringBuilder();

        // 根据子网掩码计算网段
        for (int i = 0; i < 4; i++) {
            int ipByte = Integer.parseInt(ipParts[i]);
            int maskByte = (subnetMask >> (8 * (3 - i))) & 0xFF; // 按字节移动
            int subnetByte = ipByte & maskByte;
            subnetBuilder.append(subnetByte).append(".");
        }

        // 移除最后一个点
        subnetBuilder.setLength(subnetBuilder.length() - 1);

        return subnetBuilder.toString();
    }

    // 获取网段的 IP 范围（起始 IP 和结束 IP）
    private static String[] getIpRange(String subnet, int subnetMask) {
        String[] subnetParts = subnet.split("\\.");
        String[] startIp = new String[4];
        String[] endIp = new String[4];

        for (int i = 0; i < 4; i++) {
            startIp[i] = subnetParts[i];
            endIp[i] = subnetParts[i];
        }

        // 设置最后一部分为 1 (开始 IP) 和 254 (结束 IP)
        endIp[3] = String.valueOf((subnetMask >> 24) & 0xFF);
        return new String[]{String.join(".", startIp), String.join(".", endIp)};
    }
}
