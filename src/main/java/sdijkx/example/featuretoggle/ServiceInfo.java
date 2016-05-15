package sdijkx.example.featuretoggle;

/**
 * Created by steven on 15-05-16.
 */
public class ServiceInfo {

    private final int port;
    private final String host;

    public ServiceInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }
}
