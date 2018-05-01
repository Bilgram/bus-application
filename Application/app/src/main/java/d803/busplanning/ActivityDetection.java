package d803.busplanning;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import java.util.List;

public class ActivityDetection extends IntentService{
    public String value = "";


    public ActivityDetection(){
        super("Hvad med fucking ja!");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {

        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    value = "IN_VEHICLE";
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    new ActivityReader("ON_BICYCLE", activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    new ActivityReader("ON_FOOT", activity.getConfidence());
                    break;
                }
                case DetectedActivity.RUNNING: {
                    new ActivityReader("RUNNING", activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    new ActivityReader("STILL", activity.getConfidence());
                    break;
                }
                case DetectedActivity.TILTING: {
                    new ActivityReader("TILTING", activity.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING: {
                    new ActivityReader("WALKING", activity.getConfidence());
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    new ActivityReader("UNKNOWN", activity.getConfidence());
                    break;
                }
            }
        }
    }
}
