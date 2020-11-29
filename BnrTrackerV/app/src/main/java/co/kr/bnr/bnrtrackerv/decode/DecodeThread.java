package co.kr.bnr.bnrtrackerv.decode;


import android.os.Handler;
import android.os.Looper;

import co.kr.bnr.bnrtrackerv.QrCodeActivity;

import java.util.concurrent.CountDownLatch;


/**
 * This thread does all the heavy lifting of decoding the images.
 */
public class DecodeThread extends Thread {

    private final QrCodeActivity mActivity;
    private Handler mHandler;
    private final CountDownLatch mHandlerInitLatch;

    public DecodeThread(QrCodeActivity activity) {
        this.mActivity = activity;
        mHandlerInitLatch = new CountDownLatch(1);
    }

    public Handler getHandler() {
        try {
            mHandlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return mHandler;
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new DecodeHandler(mActivity);
        mHandlerInitLatch.countDown();
        Looper.loop();
    }

}
