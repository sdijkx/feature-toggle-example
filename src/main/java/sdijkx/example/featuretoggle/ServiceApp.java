package sdijkx.example.featuretoggle;

import com.google.gson.Gson;
import sdijkx.example.featuretoggle.ads.Ad;
import sdijkx.example.featuretoggle.features.FeatureToggle;
import sdijkx.example.featuretoggle.features.FeatureToggleException;
import sdijkx.example.featuretoggle.features.Features;
import sdijkx.example.featuretoggle.orders.OrderStatus;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static spark.Spark.*;

/**
 * Created by steven on 02-05-16.
 */
public class ServiceApp {

    private static Object orderStatus;

    public static void main(String[] args) {
        try {
            //simple zookeeper example
            int port = Integer.parseInt(args[0]);
            ServiceInfo serviceInfo = new ServiceInfo(InetAddress.getLocalHost().getHostAddress(), port);

            ZookeeperClient zookeeperClient = new ZookeeperClient(args[1], "/featuretoggle-example");
            zookeeperClient.register(serviceInfo.getHost() + ":" + serviceInfo.getPort());

            FeatureToggle featureToggle = new FeatureToggle(zookeeperClient);
            featureToggle.watchFeatureMap();

            Gson gson = new Gson();
            port(port);
            get("/info", (request, response) -> serviceInfo, gson::toJson);
            get("/ads", (request, response) -> featureToggle.check(Features.SHOWADS, () -> getAds()), gson::toJson);
            post("/order", (request, response) -> featureToggle.check(Features.POSTORDER, () -> createOrder()), gson::toJson);

            exception(FeatureToggleException.class, (e, req, res) -> {
                res.status(405);
                res.body("Feature not enabled");
            });

            exception(Exception.class, (e, req, res) -> {
                res.status(500);
                res.body("Interal server error");
            });

            //close the client on exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { zookeeperClient.close(); }));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static List<Ad> getAds() {
        return Arrays.asList(new Ad("ACME", "Great Dynamite"), new Ad("ACME", "Bird cage"));
    }

    public static OrderStatus createOrder() {
        return new OrderStatus(UUID.randomUUID().toString(), OrderStatus.Status.CREATED);
    }

}
