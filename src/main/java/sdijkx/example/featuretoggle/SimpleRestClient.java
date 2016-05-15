package sdijkx.example.featuretoggle;

import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.netflix.ribbon.transport.netty.RibbonTransport;
import com.netflix.ribbon.transport.netty.http.LoadBalancingHttpClient;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by steven on 13-05-16.
 */
class SimpleRestClient {

    private final LoadBalancingHttpClient<ByteBuf, ByteBuf> client;
    private final ZoneAwareLoadBalancer<Server> loadBalancer;
    private final ZookeeperClient zookeeperClient;

    SimpleRestClient(ZookeeperClient zookeeperClient) throws Exception {

        IClientConfig clientConfig = IClientConfig.Builder.newBuilder("simple-client")
                .withConnectionManagerTimeout(500)
                .withServerListRefreshIntervalMills(50)
                .withLoadBalancerEnabled(true)
                .build();
        client = RibbonTransport.newHttpClient(clientConfig);
        loadBalancer = new ZoneAwareLoadBalancer<>();
        client.getLoadBalancerContext().setLoadBalancer(loadBalancer);
        this.zookeeperClient = zookeeperClient;
        updateLoadBalancer(zookeeperClient.getAllRegisteredServices());
        zookeeperClient.watchServices(new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if (zookeeperClient.isStarted()) {
                    try {
                        List<String> urls = zookeeperClient.getAllRegisteredServices().stream().collect(Collectors.toList());
                        System.out.println("Services changed " + (urls.stream().reduce("", (l,r) -> l + " " + r)));
                        SimpleRestClient.this.updateLoadBalancer(urls);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    zookeeperClient.watchServices(this);
                }
            }
        });
    }

    public void updateLoadBalancer(List<String> services) {
        List<Server> serverList = services.stream()
                .map(this::toURI)
                .map(uri -> new Server(uri.getHost(), uri.getPort()))
                .collect(Collectors.toList());

        loadBalancer.setServersList(serverList);
    }

    public String get(String uri) {
        HttpClientRequest<ByteBuf> httpRequest =
                HttpClientRequest
                        .createGet(uri)
                        .withHeader("X-Auth-Token", "AUTH-TOKEN");
        try {
            return client.submit(httpRequest)
                    .flatMap((response) -> response.getContent())
                    .map(byteBuf -> byteBuf.toString(Charset.defaultCharset()))
                    .toBlockingObservable()
                    .toFuture()
                    .get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private URI toURI(String uri) {
        try {
            return new URI("http://" + uri);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
