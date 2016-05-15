package sdijkx.example.featuretoggle.features;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import sdijkx.example.featuretoggle.ZookeeperClient;
import spark.Route;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by steven on 15-05-16.
 */
public class FeatureToggle {
    final ZookeeperClient zookeeperClient;
    final Map<String, String> featureMap;

    public FeatureToggle(ZookeeperClient zookeeperClient) {
        this.zookeeperClient = zookeeperClient;
        this.featureMap = getFeatureMap();
    }

    public <T> T check(Features feature, Supplier<T> supplier) {
        if (!zookeeperClient.getFeature(feature.getFeatureName()).equalsIgnoreCase("0")) {
            return supplier.get();
        } else {
            throw new FeatureToggleException(feature.getFeatureName());
        }
    }

    public void watchFeatureMap() {
        Stream.of(Features.values()).forEach(feature -> {
            zookeeperClient.watchFeature(new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    featureMap.put(feature.getFeatureName(), zookeeperClient.getFeature(feature.getFeatureName()));
                    zookeeperClient.watchFeature(this, feature.getFeatureName());
                }
            }, feature.getFeatureName());
        });
    }

    private Map<String, String> getFeatureMap() {
        return Stream.of(Features.values())
                .collect(Collectors.toConcurrentMap(
                                feature -> feature.getFeatureName(),
                                (feature) -> {
                                    zookeeperClient.createFeatureIfNotExists(feature.getFeatureName(), "0");
                                    return zookeeperClient.getFeature(feature.getFeatureName());
                                })
                );
    }
}
