package sdijkx.example.featuretoggle;

import com.google.gson.Gson;
import com.netflix.client.config.IClientConfig;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Created by steven on 02-05-16.
 */
public class FrontendApp {




    public static void main(String[] args) {
        try {
            //simple zookeeper example
            ZookeeperClient zookeeperClient = new ZookeeperClient("127.0.0.1:2181", "/featuretoggle-example");

            final Map<String, String> featureMap = zookeeperClient.getFeatureMap();
            featureMap.entrySet().stream().forEach(entry -> {
                zookeeperClient.watchFeature(new Watcher() {
                    @Override
                    public void process(WatchedEvent watchedEvent) {
                        try {
                            System.out.println("feature changed " + watchedEvent.getPath());
                            String value = zookeeperClient.getFeature(entry.getKey());
                            if (value.equals("0")) {
                                featureMap.remove(entry.getKey());
                            } else {
                                featureMap.put(entry.getKey(), value);
                            }
                            zookeeperClient.watchFeature(this, entry.getKey());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, entry.getKey());
            });


            SimpleRestClient simpleClient = new SimpleRestClient(zookeeperClient);
            Gson gson = new Gson();
            port(9090);

            get("/registered-servers", (req, res) -> zookeeperClient.getAllRegisteredServices());
            get("/feature", (req, res) -> zookeeperClient.getFeatureMap(),gson::toJson);
            post("/feature/:feature", (req, res) -> {
                zookeeperClient.setFeature(req.params("feature"), req.body());
                return zookeeperClient.getFeatureMap();
            }, gson::toJson);
            get("/service/order", (req, res) -> simpleClient.get("/order"));
            get("/service/ads", (req, res) -> simpleClient.get("/ads"));
            get("/", (req, res) -> new ModelAndView(featureMap, "index.mustache"), new MustacheTemplateEngine());

            //close the client on exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { zookeeperClient.close(); }));


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
