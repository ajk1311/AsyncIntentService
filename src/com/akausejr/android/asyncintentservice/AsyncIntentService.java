package com.akausejr.android.asyncintentservice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public abstract class AsyncIntentService extends Service {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = "AsyncIntentService";

    private String mName;
    private ExecutorService mExecutor;
    private LinkedBlockingQueue<Integer> mStartQueue;

    private boolean mRedilverIntent = false;

    public AsyncIntentService(String name) {
	super();
	mName = name;
    }

    public void setIntentRedilivery(boolean rediliverIntent) {
	mRedilverIntent = rediliverIntent;
    }

    @Override
    public void onCreate() {
	super.onCreate();
	mExecutor = createExecutor();
	mStartQueue = new LinkedBlockingQueue<Integer>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	mStartQueue.offer(startId);
	mExecutor.execute(new AsyncOperation(intent));
	return mRedilverIntent ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
	mExecutor.shutdown();
    }

    public abstract void onHandleIntent(Intent intent);

    private ExecutorService createExecutor() {
	return Executors.newCachedThreadPool(new BackgroundThreadFactory());
    }

    private class AsyncOperation implements Runnable {
	private Intent mIntent;

	public AsyncOperation(Intent intent) {
	    mIntent = intent;
	}

	@Override
	public void run() {
	    onHandleIntent(mIntent);
	    final int startId = mStartQueue.poll();
	    stopSelf(startId);
	}
    }

    private class BackgroundThreadFactory implements ThreadFactory {
	private final AtomicInteger COUNT = new AtomicInteger(1);

	@Override
	public Thread newThread(Runnable r) {
	    final Thread freshThread = new Thread(r, mName + "[" + COUNT.getAndIncrement() + "]");
	    freshThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
	    return freshThread;
	}
    }

    @Override
    public IBinder onBind(Intent intent) {
	// Not a bound service; do nothing
	return null;
    }
}
