package com.example.schoolcalenderchecker;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class UpdateWorker extends Worker {

    public UpdateWorker(@NonNull Context context,
                        @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        /*
        ContextCompat.getMainExecutor(getApplicationContext()).execute(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "WorkManager Started",
                        Toast.LENGTH_SHORT).show();
            }
        });
         */

        Log.d("WorkManagerDebug", "I did some work!");

        return Result.success();
    }
}
