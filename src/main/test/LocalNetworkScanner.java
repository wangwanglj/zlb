import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class LocalNetworkScanner {

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

        // 执行扫描
        ExecutorService executorService = Executors.newFixedThreadPool(256);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 1; i < 255; i++) {
            final String targetIp = subnet + i;
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
}
