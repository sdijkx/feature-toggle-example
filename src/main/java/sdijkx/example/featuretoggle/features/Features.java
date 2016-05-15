package sdijkx.example.featuretoggle.features;

/**
 * Created by steven on 15-05-16.
 */
public enum Features {
    SHOWADS("showAdsFeature"),
    POSTORDER("postOrderFeature");

    private final String featureName;

    Features(String featureName) {
        this.featureName = featureName;
    }

    public String getFeatureName() {
        return featureName;
    }
}
