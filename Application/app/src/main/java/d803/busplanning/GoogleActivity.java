package d803.busplanning;

import android.app.IntentService;
import android.content.Intent;
import android.os.Parcel;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.tasks.Task;

public abstract class GoogleActivity extends IntentService{

    public GoogleActivity() {
        super("GoogleActivity");
    }

    public GoogleActivity(String name){
        super(name);
    }
    @Override
    protected void onHandleIntent(Intent intent) {

    }

}
