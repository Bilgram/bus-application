package d803.busplanning;

import android.app.IntentService;
import android.content.Intent;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import java.io.IOException;
import java.util.List;
import d803.busplanning.ActivityDetection;

public class ActivityDetection extends IntentService{

    public ActivityDetection(){
        super("ActivityDetection");
    }

    public ActivityDetection(String name){
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            try {
                handleDetectedActivities(result.getProbableActivities());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) throws IOException {
        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    new ActivityReader( "IN_VEHICLE",
                            new float[]{activity.getConfidence()}, System.currentTimeMillis());
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    new ActivityReader( "ON_BICYCLE",
                            new float[]{activity.getConfidence()}, System.currentTimeMillis());
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    new ActivityReader( "ON_FOOT",
                            new float[]{activity.getConfidence()}, System.currentTimeMillis());
                    break;
                }
                case DetectedActivity.RUNNING: {
                    new ActivityReader( "RUNNING",
                            new float[]{activity.getConfidence()}, System.currentTimeMillis());
                    break;
                }
                case DetectedActivity.STILL: {
                    new ActivityReader( "STILL",
                            new float[]{activity.getConfidence()}, System.currentTimeMillis());
                    break;
                }
                case DetectedActivity.TILTING: {
                    new ActivityReader( "TILTING",
                            new float[]{activity.getConfidence()}, System.currentTimeMillis());
                    break;
                }
                case DetectedActivity.WALKING: {
                    new ActivityReader( "WALKING",
                            new float[]{activity.getConfidence()}, System.currentTimeMillis());
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    new ActivityReader( "UNKNOWN",
                            new float[]{activity.getConfidence()}, System.currentTimeMillis());
                    break;
                }
            }
        }
    }
}
