package tv.utao.x5;

import android.content.Intent;
import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import tv.utao.x5.databinding.DialogExitMainBinding;
import tv.utao.x5.impl.WebViewClientImpl;
import tv.utao.x5.util.LogUtil;
import tv.utao.x5.util.ValueUtil;
import tv.utao.x5.utils.ToastUtils;
import tv.utao.x5.util.WebViewDispatcher;

public class MainActivity extends BaseWebViewActivity {
    private long mClickBackTime = 0;
    private DialogExitMainBinding exitDialogBinding;
    private boolean isExitDialogShowing = false;
    private boolean x5Ok(){
        return "ok".equals(ValueUtil.getString(this,"x5","0"));
    }
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            float x= event.getX();
            float y= event.getY();
            //LogUtil.i("dispatchTouchEvent", "x" + x+"y "+y);
            if(x<100f&&y<100f) {
                ctrl("menu");
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean ctrl(String code){
        if (mWebView != null) {
            String  js= "_menuCtrl."+code+"()";
            LogUtil.i(TAG,js);
            mWebView.evaluateJavascript(js,null);
        }
        return true;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LogUtil.i(TAG,"onConfigurationChanged...."+newConfig.orientation);
        super.onConfigurationChanged(newConfig);
        // 检查屏幕方向是否改变
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 在这里处理横屏模式下的布局调整
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 在这里处理竖屏模式下的布局调整
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebViewDispatcher.registerLoadUrlCallback(new WebViewDispatcher.LoadUrlCallback() {
            @Override
            public void accept(String u) {
                if (mWebView != null) {
                    mWebView.loadUrl(u);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        WebViewDispatcher.unregister();
        super.onPause();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event);
        }
        int keyCode = event.getKeyCode();
        LogUtil.i("keyDown keyCode ", keyCode+" event" + event);
        
        // 优先处理退出对话框
        if(isExitDialogShowing){
            if(keyCode==KeyEvent.KEYCODE_BACK){
                finish();
                return true;
            }
            // 退出对话框显示时，让系统处理上下键焦点切换和确认键
            if(keyCode==KeyEvent.KEYCODE_DPAD_UP || keyCode==KeyEvent.KEYCODE_DPAD_DOWN || 
               keyCode==KeyEvent.KEYCODE_DPAD_CENTER || keyCode==KeyEvent.KEYCODE_ENTER){
                return super.dispatchKeyEvent(event);
            }
            // 其他按键不处理
            return true;
        }
        
        boolean isMenuShow=isMenuShow();
        if(isMenuShow){
            if(keyCode==KeyEvent.KEYCODE_BACK||keyCode==KeyEvent.KEYCODE_MENU||keyCode==KeyEvent.KEYCODE_TAB){
                hideMenu();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if(keyCode==KeyEvent.KEYCODE_BACK){
            return keyBack();
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_ENTER){
            if(openOkMenu()&&!WebViewClientImpl.currentUrlIsHome()){
                return ctrl("menu");
            }
            return ctrl("ok");
           // return super.dispatchKeyEvent(event);
        }
        if(keyCode==KeyEvent.KEYCODE_MENU||keyCode==KeyEvent.KEYCODE_TAB){
            return ctrl("menu");
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
            return ctrl("right");
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
            return ctrl("left");
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            return ctrl("down");
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_UP){
            return ctrl("up");
        }
        if(keyCode==KeyEvent.KEYCODE_VOLUME_UP||keyCode==KeyEvent.KEYCODE_VOLUME_DOWN
                ||keyCode==KeyEvent.KEYCODE_VOLUME_MUTE){
            return super.dispatchKeyEvent(event);
        }
       // return ctrl("menu");
        return super.dispatchKeyEvent(event);
    }
    private boolean keyBack(){
        // 如果退出对话框已显示，再次按返回键则退出
        if (isExitDialogShowing) {
            finish();
            return true;
        }
        
        String url = WebViewClientImpl.backUrl();
        LogUtil.i("keyBack","keyBack "+url);
        //NextPlusNavigationDelegate.backUrl();
        if(null!=url&&null!=mWebView){
            mWebView.loadUrl(url);
            return true;
        }
        
        // 显示退出对话框
        showExitDialog();
        return true;
    }
    
    private void showExitDialog() {
        if (exitDialogBinding == null) {
            initExitDialog();
        }
        isExitDialogShowing = true;
        exitDialogBinding.exitDialogContainer.setVisibility(View.VISIBLE);
        
        // 设置对话框中按钮的焦点
        //exitDialogBinding.btnExit.setFocusable(true);
        exitDialogBinding.btnCancel.setFocusable(true);
        exitDialogBinding.btnStartToggle.setFocusable(true);
        
        // 设置启动按钮文案（启动XX）
        String currentStartPage = ValueUtil.getString(this, "startPage", "main");
        if ("main".equals(currentStartPage)) {
            exitDialogBinding.btnStartToggle.setText("启动即电视直播");
        } else {
            exitDialogBinding.btnStartToggle.setText("启动即视频点播");
        }
        
        // 默认焦点在退出按钮上
        exitDialogBinding.btnCancel.post(() -> exitDialogBinding.btnCancel.requestFocus());

        // Main 返回菜单不显示收藏与画质
        // 此布局本身不包含这两个控件，无需隐藏
        
        // 设置左侧二维码图片
        try {
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(getAssets().open("tv-web/img/myzsm.jpg"));
            exitDialogBinding.qrDonate.setImageBitmap(bmp);
        } catch (Throwable ignore) {}

        // 默认焦点设置到“取消”按钮
        try {
            exitDialogBinding.btnCancel.setFocusable(true);
            exitDialogBinding.btnCancel.setFocusableInTouchMode(true);
            exitDialogBinding.btnCancel.post(() -> exitDialogBinding.btnCancel.requestFocus());
        } catch (Throwable ignore) {}

        // 根据X5开关状态：已开启则隐藏按钮；未开启则显示“开启X5内核(会关闭应用)”
        try {
            if (x5Ok()) {
                exitDialogBinding.btnOpenX5.setVisibility(View.GONE);
            } else {
                exitDialogBinding.btnOpenX5.setVisibility(View.VISIBLE);
                exitDialogBinding.btnOpenX5.setText("开启X5内核(会关闭应用)");
            }
        } catch (Throwable ignore) {}

        // 自启动按钮文案（保留逻辑，但按钮已隐藏）
        try {
            String autoStart = ValueUtil.getString(this, "autoStart", "0");
            if ("1".equals(autoStart)) {
                exitDialogBinding.btnAutoStart.setText("关闭自启动");
            } else {
                exitDialogBinding.btnAutoStart.setText("开启自启动");
            }
            exitDialogBinding.btnAutoStart.setVisibility(View.GONE);
        } catch (Throwable ignore) {}

        // 焦点链调整：启动 -> (X5 可见则到 X5) -> 取消（自启动按钮隐藏）
        try {
            boolean x5Visible = exitDialogBinding.btnOpenX5.getVisibility() == View.VISIBLE;
            if (x5Visible) {
                exitDialogBinding.btnStartToggle.setNextFocusDownId(exitDialogBinding.btnOpenX5.getId());
                exitDialogBinding.btnOpenX5.setNextFocusUpId(exitDialogBinding.btnStartToggle.getId());
                exitDialogBinding.btnOpenX5.setNextFocusDownId(exitDialogBinding.btnCancel.getId());
            } else {
                exitDialogBinding.btnStartToggle.setNextFocusDownId(exitDialogBinding.btnCancel.getId());
            }
            exitDialogBinding.btnCancel.setNextFocusUpId(exitDialogBinding.btnStartToggle.getId());
        } catch (Throwable ignore) {}
    }
    
    private void hideExitDialog() {
        if (exitDialogBinding != null) {
            isExitDialogShowing = false;
            exitDialogBinding.exitDialogContainer.setVisibility(View.GONE);
        }
    }
    
    private void initExitDialog() {
        View dialogView = findViewById(R.id.exitDialog);
        exitDialogBinding = DataBindingUtil.bind(dialogView);
        
        if (exitDialogBinding == null) {
            return;
        }

        // 取消按钮
        exitDialogBinding.btnCancel.setOnClickListener(v -> {
            hideExitDialog();
        });
        
        // 点击背景关闭对话框
        exitDialogBinding.dialogBackdrop.setOnClickListener(v -> {
            hideExitDialog();
        });
        
        // 启动首页切换按钮（仅按钮，点击切换并更新文案）
        exitDialogBinding.btnStartToggle.setOnClickListener(v -> {
            String currentStartPage = ValueUtil.getString(this, "startPage", "main");
            if ("main".equals(currentStartPage)) {
                // 当前是视频点播，切换到电视直播
                ValueUtil.putString(this, "startPage", "live");
                ToastUtils.show(this, "已设置启动首页为：电视直播", Toast.LENGTH_SHORT);
                exitDialogBinding.btnStartToggle.setText("启动即视频点播");
            } else {
                // 当前是电视直播，切换到视频点播
                ValueUtil.putString(this, "startPage", "main");
                ToastUtils.show(this, "已设置启动首页为：视频点播", Toast.LENGTH_SHORT);
                exitDialogBinding.btnStartToggle.setText("启动即电视直播");
            }
        });

        // 开启X5按钮（无关闭功能）
        exitDialogBinding.btnOpenX5.setOnClickListener(v -> {
            ValueUtil.putString(getApplicationContext(), "openX5", "1");
            ToastUtils.show(this, "已开启X5，将重启应用", Toast.LENGTH_SHORT);
            finishAffinity();
            System.exit(0);
        });

        // 开启自启动按钮（切换开关，也可进入设置页）
        try {
            exitDialogBinding.btnAutoStart.setOnClickListener(v -> {
                String current = ValueUtil.getString(this, "autoStart", "0");
                if ("1".equals(current)) {
                    ValueUtil.putString(this, "autoStart", "0");
                    exitDialogBinding.btnAutoStart.setText("开启自启动");
                    ToastUtils.show(this, "已关闭自启动", Toast.LENGTH_SHORT);
                } else {
                    ValueUtil.putString(this, "autoStart", "1");
                    exitDialogBinding.btnAutoStart.setText("关闭自启动");
                    ToastUtils.show(this, "已开启自启动", Toast.LENGTH_SHORT);
                }
            });
        } catch (Throwable ignore) {}
    }

    // 已移除设置页跳转，保留占位以避免方法引用丢失
    
}
