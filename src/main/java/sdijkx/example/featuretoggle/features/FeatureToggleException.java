package sdijkx.example.featuretoggle.features;

/**
 * Created by steven on 15-05-16.
 */
public class FeatureToggleException extends RuntimeException {
    public FeatureToggleException(String featureName) {
        super(featureName + " not enabled");
    }
}
