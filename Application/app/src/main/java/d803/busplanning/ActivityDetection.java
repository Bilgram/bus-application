package d803.busplanning;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import java.util.List;

public class ActivityDetection extends IntentService{
    public ActivityDetection(){
        super("jhedeg");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        SharedPreferences preferences = getSharedPreferences("ActivityRecognition", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    editor.putString("Activity", "In Vehicle");
                    editor.apply();
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    editor.putString("Activity", "On Bicycle");
                    editor.apply();
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    editor.putString("Activity", "On Foot");
                    editor.apply();
                    break;
                }
                case DetectedActivity.RUNNING: {
                    editor.putString("Activity", "Running");
                    editor.apply();
                    break;
                }
                case DetectedActivity.STILL: {
                    editor.putString("Activity", "Still");
                    editor.apply();
                    break;
                }
                case DetectedActivity.TILTING: {
                    editor.putString("Activity", "Tilting");
                    editor.apply();
                    break;
                }
                case DetectedActivity.WALKING: {
                    editor.putString("Activity", "Walking");
                    editor.apply();
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    editor.putString("Activity", "unknown");
                    editor.apply();
                    break;
                }
            }
        }
    }
}
