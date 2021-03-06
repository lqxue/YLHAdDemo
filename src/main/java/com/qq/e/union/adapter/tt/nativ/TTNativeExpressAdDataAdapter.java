package com.qq.e.union.adapter.tt.nativ;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.ads.nativ.NativeExpressMediaListener;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADEventListener;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.compliance.DownloadConfirmCallBack;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.pi.AdData;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.adapter.util.Constant;

import java.util.HashMap;
import java.util.Map;

import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_AD_CLICKED;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_AD_CLOSED;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_AD_EXPOSURE;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_RENDER_FAILED;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_RENDER_SUCCESS;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_VIDEO_CACHED;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_VIDEO_COMPLETE;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_VIDEO_ERROR;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_VIDEO_PAUSE;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_VIDEO_START;

public class TTNativeExpressAdDataAdapter extends NativeExpressADView implements ADEventListener {
  private static final String TAG = TTNativeExpressAdDataAdapter.class.getSimpleName();

  private TTNativeExpressAd mTTNativeExpressAd;
  private NativeExpressMediaListener mNativeExpressMediaListener;
  private final AdData mAdData;
  private ADListener mListener;
  private final Context mContext;
  private String mEcpmLevel;
  private boolean mIsExposed;

  public TTNativeExpressAdDataAdapter(Context context, TTNativeExpressAd data) {
    super(context);
    this.mTTNativeExpressAd = data;
    mContext = context;
    // ????????? close ????????? dislike ???????????????
    bindDislike(data);
    tryBindInteractionAdListener();
    tryBindVideoAdListener();
    mAdData = new AdData() {
      @Override
      public String getTitle() {
        return null;
      }

      @Override
      public String getDesc() {
        return null;
      }

      /**
       *  ?????????????????????????????????????????????????????????????????????????????????????????????
       */
      @Override
      public int getAdPatternType() {
        switch (data.getImageMode()) {
          case TTAdConstant.IMAGE_MODE_VIDEO:
          case TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL:
            return AdPatternType.NATIVE_VIDEO;
          case TTAdConstant.IMAGE_MODE_GROUP_IMG:
            return AdPatternType.NATIVE_3IMAGE;
          case TTAdConstant.IMAGE_MODE_LARGE_IMG:
            return AdPatternType.NATIVE_2IMAGE_2TEXT;
          case TTAdConstant.IMAGE_MODE_SMALL_IMG:
          default:
            return AdPatternType.NATIVE_1IMAGE_2TEXT;
        }
      }

      @Override
      public int getECPM() {
        return TTNativeExpressAdDataAdapter.this.getECPM();
      }

      @Override
      public String getECPMLevel() {
        return mEcpmLevel;
      }

      @Override
      public void setECPMLevel(String level) {
        mEcpmLevel = level;
      }

      @Override
      public Map<String, Object> getExtraInfo() {
        return new HashMap<>();
      }

      @Override
      public String getProperty(String property) {
        return null;
      }

      @Override
      public <T> T getProperty(Class<T> type) {
        return null;
      }

      @Override
      public boolean equalsAdData(AdData adData) {
        return false;
      }

      @Override
      public int getVideoDuration() {
        return 0;
      }
    };
  }

  @Override
  public void render() {
    mTTNativeExpressAd.render();
  }

  @Override
  public void destroy() {
    if (mTTNativeExpressAd != null) {
      mTTNativeExpressAd.destroy();
      mTTNativeExpressAd = null;
    }
  }

  @Override
  public AdData getBoundData() {
    return mAdData;
  }

  private void tryBindInteractionAdListener() {
    mTTNativeExpressAd.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
      @Override
      public void onAdClicked(View view, int type) {
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLICKED,
            new Object[]{TTNativeExpressAdDataAdapter.this, ""}));
      }

      @Override
      public void onAdShow(View view, int type) {
        if (mListener == null && !mIsExposed) {
          return;
        }
        mIsExposed = true;
        mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_EXPOSURE,
            new Object[]{TTNativeExpressAdDataAdapter.this}));
      }

      @Override
      public void onRenderFail(View view, String msg, int code) {
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_RENDER_FAILED,
            new Object[]{TTNativeExpressAdDataAdapter.this}));
      }

      @Override
      public void onRenderSuccess(View view, float width, float height) {
        mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_RENDER_SUCCESS,
            new Object[]{TTNativeExpressAdDataAdapter.this}));
        post(() -> addView(mTTNativeExpressAd.getExpressAdView()));
      }
    });
  }

  private void tryBindVideoAdListener() {
    mTTNativeExpressAd.setVideoAdListener(new TTNativeExpressAd.ExpressVideoAdListener() {
      @Override
      public void onVideoLoad() {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoCached(TTNativeExpressAdDataAdapter.this);
        }
      }

      @Override
      public void onVideoError(int errorCode, int extraCode) {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoError(TTNativeExpressAdDataAdapter.this,
              new AdError(errorCode, ""));
        }
      }

      @Override
      public void onVideoAdStartPlay() {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoStart(TTNativeExpressAdDataAdapter.this);
        }
      }

      @Override
      public void onVideoAdPaused() {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoPause(TTNativeExpressAdDataAdapter.this);
        }
      }

      @Override
      public void onVideoAdComplete() {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoComplete(TTNativeExpressAdDataAdapter.this);
        }
      }

      @Override
      public void onVideoAdContinuePlay() {

      }

      @Override
      public void onProgressUpdate(long t, long l1) {

      }

      @Override
      public void onClickRetry() {

      }
    });
  }

  private void bindDislike(TTNativeExpressAd ad) {
    //???????????????????????????dislike????????????
    ad.setDislikeCallback((Activity) mContext, new TTAdDislike.DislikeInteractionCallback() {
      @Override
      public void onShow() {
        Log.d(TAG, "???????????????????????????");
      }

      @Override
      public void onSelected(int position, String value, boolean enforce) {
        //???????????????????????????????????????????????????
        Log.d(TAG, "????????????");
        if (mListener != null) {
          mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLOSED,
              new Object[]{TTNativeExpressAdDataAdapter.this}));
        }
        removeView(mTTNativeExpressAd.getExpressAdView());
        if (enforce) {
          Log.d(TAG, "??????????????????");
        }
      }

      @Override
      public void onCancel() {

      }

    });
  }

  @Override
  public void setAdListener(ADListener listener) {
    mListener = listener;
  }

  @Override
  public void setMediaListener(NativeExpressMediaListener mediaListener) {
    mNativeExpressMediaListener = mediaListener;
  }

  @Override
  public String getECPMLevel() {
    return mAdData.getECPMLevel();
  }

  /**
   * ======================================================================
   * ????????????????????????
   */

  @Override
  public int getECPM() {
    return Constant.VALUE_NO_ECPM;
  }

  @Override
  public void preloadVideo() {

  }

  @Override
  public void sendWinNotification(int price) {
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {

  }

  @Override
  public void setBidECPM(int price) {

  }

  @Override
  public void onDownloadConfirm(Activity context, int scenes, String infoUrl,
                                DownloadConfirmCallBack callBack) {

  }

  @Override
  public String getApkInfoUrl() {
    return null;
  }

  @Override
  public void setDownloadConfirmListener(DownloadConfirmListener listener) {

  }

  @Override
  public void setViewBindStatusListener(ViewBindStatusListener listener) {

  }

  @Override
  public void negativeFeedback() {

  }

  @Override
  public void setAdSize(ADSize adSize) {

  }
}
