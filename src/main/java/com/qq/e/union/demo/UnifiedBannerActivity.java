package com.qq.e.union.demo;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.qq.e.ads.banner2.UnifiedBannerADListener;
import com.qq.e.ads.banner2.UnifiedBannerView;
import com.qq.e.comm.constants.BiddingLossReason;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.view.S2SBiddingDemoUtils;

import java.util.Locale;


public class UnifiedBannerActivity extends Activity implements OnClickListener,
    UnifiedBannerADListener {

  private static final String TAG = UnifiedBannerActivity.class.getSimpleName();
  ViewGroup mBannerContainer;
  UnifiedBannerView mBannerView;
  String mCurrentPosId;
  String mS2SBiddingToken;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_unified_banner);
    mBannerContainer = (ViewGroup) this.findViewById(R.id.bannerContainer);
    ((EditText) findViewById(R.id.posId)).setText(PositionId.UNIFIED_BANNER_POS_ID);
    this.findViewById(R.id.refreshBanner).setOnClickListener(this);
    this.findViewById(R.id.closeBanner).setOnClickListener(this);
    this.getBanner().loadAD();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mBannerView != null) {
      mBannerView.destroy();
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mBannerView != null) {
      mBannerView.setLayoutParams(getUnifiedBannerLayoutParams());
    }
  }

  protected UnifiedBannerView getBanner() {
    String editPosId = getPosID();
    if (mBannerView == null || !editPosId.equals(mCurrentPosId) || !TextUtils.isEmpty(mS2SBiddingToken)) {
      if(this.mBannerView != null){
        mBannerView.destroy();
      }
      Log.d(TAG, "getBanner: BiddingToken " + mS2SBiddingToken);
      if (!TextUtils.isEmpty(mS2SBiddingToken)) {
        mBannerView = new UnifiedBannerView(this, editPosId, this, null, mS2SBiddingToken);
      } else {
        mBannerView = new UnifiedBannerView(this, editPosId, this);
      }
      mBannerView.setLoadAdParams(DemoUtil.getLoadAdParams("banner"));
      mCurrentPosId = editPosId;
      mBannerContainer.removeAllViews();
      mBannerContainer.addView(mBannerView, getUnifiedBannerLayoutParams());
    }
    if (((CheckBox) findViewById(R.id.cbRefreshInterval)).isChecked()) {
      try {
        int refreshInterval = Integer.parseInt(((EditText) findViewById(R.id.etRefreshInterval))
            .getText().toString());
        this.mBannerView.setRefresh(refreshInterval);
      } catch (NumberFormatException e) {
        Toast.makeText(this, "????????????????????????????????????!", Toast.LENGTH_LONG).show();
      }
    } else {
      // ?????? 30 ???????????????????????????
      this.mBannerView.setRefresh(30);
    }
    return this.mBannerView;
  }

  /**
   * banner2.0??????banner??????????????????6.4:1 , ????????????????????????????????????????????????????????????????????????
   *
   * @return
   */
  private FrameLayout.LayoutParams getUnifiedBannerLayoutParams() {
    Point screenSize = new Point();
    getWindowManager().getDefaultDisplay().getSize(screenSize);
    return new FrameLayout.LayoutParams(screenSize.x,  Math.round(screenSize.x / 6.4F));
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.refreshBanner:
        doRefreshBanner();
        break;
      case R.id.closeBanner:
        doCloseBanner();
        break;
      default:
        break;
    }
  }

  public void requestS2SBiddingToken(View view) {
    S2SBiddingDemoUtils.requestBiddingToken(this, getPosID(),
        GDTAdSdk.getGDTAdManger().getBuyerId(), token -> {
      mS2SBiddingToken = token;
    });
  }

  private void doRefreshBanner() {
    DemoUtil.hideSoftInput(this);
    getBanner().loadAD();
  }

  private void doCloseBanner() {
    mBannerContainer.removeAllViews();
    if (mBannerView != null) {
      mBannerView.destroy();
      mBannerView = null;
    }
  }

  private String getPosID() {
    String posId = ((EditText) findViewById(R.id.posId)).getText().toString();
    return TextUtils.isEmpty(posId) ? PositionId.BANNER_POS_ID : posId;
  }

  @Override
  public void onNoAD(AdError adError) {
    String msg = String.format(Locale.getDefault(), "onNoAD, error code: %d, error msg: %s",
        adError.getErrorCode(), adError.getErrorMsg());
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onADReceive() {
    if (mBannerView != null) {
      Log.i(TAG, "onADReceive" + ", ECPM: " + mBannerView.getECPM() + ", ECPMLevel: "
          + mBannerView.getECPMLevel() + ", adNetWorkName: " + mBannerView.getAdNetWorkName()
          + ", testExtraInfo:" + mBannerView.getExtraInfo().get("mp"));
      if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
        mBannerView.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
      }
      reportBiddingResult(mBannerView);
    }
  }

  /**
   * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
   *
   * ??????????????????????????? sendLossNotification??????????????????????????????????????????????????????ADN ID?????????????????????ADN??????????????????
   * ??????????????????????????? sendWinNotification??????????????????????????????????????????????????????
   * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
   */
  private void reportBiddingResult(UnifiedBannerView unifiedBannerView) {
    if (DemoUtil.isReportBiddingLoss() == DemoUtil.REPORT_BIDDING_LOSS) {
      unifiedBannerView.sendLossNotification(100, BiddingLossReason.LOW_PRICE, "WinAdnID");
    } else if (DemoUtil.isReportBiddingLoss() == DemoUtil.REPORT_BIDDING_WIN) {
      unifiedBannerView.sendWinNotification(200);
    }
    if (DemoUtil.isNeedSetBidECPM()) {
      unifiedBannerView.setBidECPM(300);
    }
  }

  @Override
  public void onADExposure() {
    Log.i(TAG, "onADExposure");
  }

  @Override
  public void onADClosed() {
    Log.i(TAG, "onADClosed");
  }

  @Override
  public void onADClicked() {
    Log.i(TAG, "onADClicked : " + (mBannerView.getExt() != null? mBannerView.getExt().get("clickUrl") : ""));
  }

  @Override
  public void onADLeftApplication() {
    Log.i(TAG, "onADLeftApplication");
  }

  @Override
  public void onADOpenOverlay() {
    Log.i(TAG, "onADOpenOverlay");
  }

  @Override
  public void onADCloseOverlay() {
    Log.i(TAG, "onADCloseOverlay");
  }
}
