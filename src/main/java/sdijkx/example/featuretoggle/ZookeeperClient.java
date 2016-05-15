package sdijkx.example.featuretoggle;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by steven on 03-05-16.
 */
public class ZookeeperClient {

    private final CuratorFramework curatorFramework;
    private final String rootZkNode;

    public ZookeeperClient(String zookeeperHost, String namespace) {
        try {
            curatorFramework = CuratorFrameworkFactory
                    .builder().namespace(namespace)
                    .retryPolicy(new RetryNTimes(3, 100))
                    .connectString(zookeeperHost).build();
            curatorFramework.start();
            curatorFramework.blockUntilConnected();
            this.rootZkNode = namespace;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void register(String url) throws Exception {
        String zkNode = getServicesZkNode() + "/_";
        String znodePath = curatorFramework
                .create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(zkNode, url.getBytes());
    }

    public List<String> getAllRegisteredServices() throws Exception {
        String zkNode = getServicesZkNode();
        return curatorFramework.getChildren().forPath(zkNode).stream().map(path -> {
            try {
                return new String(curatorFramework.getData().forPath(ZKPaths.makePath(zkNode, path)));
            } catch (Exception e) {
                return null;
            }
        })
                .filter(uri -> uri != null)
                .collect(Collectors.toList());
    }

    public void watchServices(Watcher watcher) {
        try {
            String zkNode = getServicesZkNode();
            curatorFramework.getChildren().usingWatcher(watcher).forPath(zkNode);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Map<String, String> getFeatureMap() throws Exception {
        String zkNode = getFeatureMapNode();
        return curatorFramework.getChildren().forPath(zkNode).stream().map(path -> {
            try {
                return new String[]{path, new String(curatorFramework.getData().forPath(ZKPaths.makePath(zkNode, path)))};
            } catch (Exception e) {
                return null;
            }
        })
                .filter(entry -> entry != null)
                .collect(Collectors.toConcurrentMap(entry -> entry[0], entry -> entry[1], (a, b) -> a));
    }

    public void createFeatureIfNotExists(String feature, String value) {
        try {
            String zkNode = getFeatureMapNode();
            String featurePath = ZKPaths.makePath(zkNode, feature);
            if(curatorFramework.checkExists().forPath(featurePath) == null ) {
                curatorFramework.create().forPath(featurePath, value.getBytes("UTF-8"));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void setFeature(String feature, String value) {
        createFeatureIfNotExists(feature, "0");
        try {
            String zkNode = getFeatureMapNode();
            String featurePath = ZKPaths.makePath(zkNode, feature);
            curatorFramework.setData().forPath(featurePath, value.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public void watchFeature(Watcher watcher, String featureToggle) {
        try {
            String zkNode = getFeatureMapNode();
            curatorFramework.getData().usingWatcher(watcher).forPath(ZKPaths.makePath(zkNode, featureToggle));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String getFeature(String path) {
        try {
            String zkNode = getFeatureMapNode();
            byte[] data = curatorFramework.getData().forPath(ZKPaths.makePath(zkNode, path));
            return new String(data, Charset.forName("UTF-8"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void close() {
        curatorFramework.close();
    }

    public boolean isStarted() {
        return curatorFramework.getState() == CuratorFrameworkState.STARTED;
    }

    private String getServicesZkNode() {
        return "/services";
    }

    private String getFeatureMapNode() {
        return "/features";
    }

}
