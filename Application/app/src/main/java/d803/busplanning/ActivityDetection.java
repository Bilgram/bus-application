package d803.busplanning;


import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

public class ActivityDetection extends IntentService {
    public ActivityDetection() {
        super("My Activity Recognition Service");
    }

    private DetectedActivity getVehicle(List<DetectedActivity> detectedActivitylist){
        for (DetectedActivity da : detectedActivitylist){
            int t = da.getType();
            if (t == 0)
                return da;
        }
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            List<DetectedActivity> res = result.getProbableActivities();
            DetectedActivity vehicle = getVehicle(res);
            Intent i = new Intent("com.kpbird.myactivityrecognition.ACTIVITY_RECOGNITION_DATA");
            i.putExtra("Activity", getType(result.getMostProbableActivity().getType()) );
            i.putExtra("Confidence", result.getMostProbableActivity().getConfidence());
            if (vehicle != null) {
                i.putExtra("vehicle confidence", vehicle.getConfidence());
                i.putExtra("vehicle", getType(vehicle.getType()));
            }else{
                i.putExtra("vehicle confidence", 1);
                i.putExtra("vehicle", "Nothing here");
            }
            sendBroadcast(i);
        }
    }

    private String getType(int type){
        if(type == DetectedActivity.UNKNOWN)
            return "Unknown";
        else if(type == DetectedActivity.IN_VEHICLE)
            return "In Vehicle";
        else if(type == DetectedActivity.ON_BICYCLE)
            return "On Bicycle";
        else if(type == DetectedActivity.ON_FOOT)
            return "On Foot";
        else if(type == DetectedActivity.STILL)
            return "Still";
        else if(type == DetectedActivity.TILTING)
            return "Tilting";
        else
            return "";
    }
}
