package tv.utao.x5;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.gson.reflect.TypeToken;
import com.tencent.smtt.export.external.extension.interfaces.IX5WebSettingsExtension;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.export.external.interfaces.PermissionRequest;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tv.utao.x5.call.StringCallback;
import tv.utao.x5.dao.HistoryDaoX;
import tv.utao.x5.databinding.ActivityLiveBinding;
import tv.utao.x5.databinding.ItemHzLiveBinding;
import tv.utao.x5.domain.HzItem;
import tv.utao.x5.databinding.DialogExitBinding;
import tv.utao.x5.domain.live.DataWrapper;
import tv.utao.x5.impl.BaseBindingAdapter;
import tv.utao.x5.impl.BaseViewHolder;
import tv.utao.x5.domain.live.Live;
import tv.utao.x5.domain.live.Vod;
import tv.utao.x5.impl.WebViewClientImpl;
import tv.utao.x5.impl.X5WebChromeClientExtension;
import tv.utao.x5.service.FavoriteService;
import tv.utao.x5.service.UpdateService;
import tv.utao.x5.util.FileUtil;
import tv.utao.x5.util.HttpUtil;
import tv.utao.x5.util.JsonUtil;
import tv.utao.x5.util.LogUtil;
import tv.utao.x5.util.Util;
import tv.utao.x5.util.ValueUtil;
import tv.utao.x5.utils.ToastUtils;

public class LiveActivity extends BaseActivity {
    protected String TAG = "LiveActivity";

    protected ActivityLiveBinding binding;
    private Context thisContext;
    private static Vod currentLive = null;
    private List<Live> provinces = new ArrayList<>();
    private int currentProvinceIndex = 0;
    private DialogExitBinding exitDialogBinding;
    private boolean isExitDialogShowing = false;
    
    // 添加收藏服务
    private FavoriteService favoriteService;
    private boolean x5Ok(){
        return "ok".equals(ValueUtil.getString(this,"x5","0"));
    }




    @Override
    protected void createInit() {
        bind();
        UpdateService.baseFolder= this.getFilesDir().getPath();
        UpdateService.updateRes(this);
        UpdateService.initTvData();
        thisContext=this;
        if(null==currentLive){
            currentLive = HistoryDaoX.currentChannel(this);
            //UpdateService.getByKey("0_0");
        }
        if(null==currentLive){
            ToastUtils.show(this,"获取数据错误 请重启",Toast.LENGTH_SHORT);
            finish();
            return;
        }
        initData();
        //更新数据
        initWebView();
        mWebView.requestFocus();
        binding.webviewWrapper.requestFocus();
        //数据库获取最新数据
        //String liveUrl= "https://tv.cctv.com/live/cctv13/";
        mWebView.loadUrl(currentLive.getUrl());
        ToastUtils.show(this,"已支持遥控器上下左右可快速切台",Toast.LENGTH_SHORT);
    }

    private long lastTime = 0;
    protected void initWebViewClient() {
        mWebView.setWebViewClient(new WebViewClientImpl(getBaseContext(),mWebView,1));
    }
    // 在 Handler 对象中处理消息
   private Handler  handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    String messageContent = (String) msg.obj;
                    // 处理接收到的消息（例如，显示 Toast）
                    if(currentLive.getKey().equals(messageContent)){
                        if (mWebView != null) {
                            mWebView.loadUrl(currentLive.getUrl());
                        }
                        //记录到db
                    }
                    break;
                case 2:
                    binding.liveName.setText("");
                    break;
            }
        }
    };
    /*
    说明：我们使用 obtainMessage 方法创建消息，并将消息的内容（字符串）作为第二个参数。
    */
    private boolean goNext(String nextType){
        if(null==currentLive){
            currentLive = UpdateService.getByKey("0_0");
        }
        String key= UpdateService.liveNext(currentLive.getTagIndex(),currentLive.getDetailIndex(),nextType);
        currentLive = UpdateService.getByKey(key);
        if(null!=currentLive){
            //延迟1s
            showToast(currentLive.getName(),this);
            String liveKey=currentLive.getKey();
            handler.sendMessageDelayed (handler.obtainMessage(1, liveKey),1000);

        }
        return true;
    }
    protected   void showToast(String text, Context context){
        binding.liveName.setText(text);
        //showToastOrg(text,context);
    }


    public boolean dispatchTouchEvent(MotionEvent event) {
        if(!isMenuShow()&&event.getAction() == KeyEvent.ACTION_DOWN){
            showMenu();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
    protected     boolean isMenuShow(){
        int visible=  binding.menuContainer.getVisibility();
        if(visible== View.VISIBLE){
            return true;
        }
        return false;
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
            // 退出对话框显示时，让系统处理上下键焦点切换
            if(keyCode==KeyEvent.KEYCODE_DPAD_UP || keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                return super.dispatchKeyEvent(event);
            }
            // 其他按键也交给系统处理（如确认键）
            return super.dispatchKeyEvent(event);
        }
        
        boolean isMenuShow=isMenuShow();
        if(isMenuShow){
            if(keyCode==KeyEvent.KEYCODE_BACK||keyCode==KeyEvent.KEYCODE_MENU||keyCode==KeyEvent.KEYCODE_TAB){
                hideMenu();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    currentProvinceIndex--;
                    if (currentProvinceIndex < 0) {
                        currentProvinceIndex = provinces.size() - 1;
                    }
                } else {
                    currentProvinceIndex++;
                    if (currentProvinceIndex >= provinces.size()) {
                        currentProvinceIndex = 0;
                    }
                }
                showCurrentProvince();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if(keyCode==KeyEvent.KEYCODE_MENU|| keyCode == KeyEvent.KEYCODE_TAB||keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_ENTER){
            showMenu();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return goNext("right");
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            return goNext("left");
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            return goNext("down");
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            return goNext("up");
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackPress();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    
    private void handleBackPress(){
        // 不再显示悬浮画质层，直接处理退出
        // 如果退出对话框已显示，再次按返回键则退出
        if (isExitDialogShowing) {
            finish();
            return;
        }
        // 显示退出对话框
        showExitDialog();
    }
    
    private void toHome(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void showExitDialog() {
        if (exitDialogBinding == null) {
            initExitDialog();
        }
        isExitDialogShowing = true;
        exitDialogBinding.exitDialogContainer.setVisibility(View.VISIBLE);
        // 初始化退出对话框中的画质列表（模拟）
        setupHzListInExit();
        
        // 设置对话框中按钮的焦点
        exitDialogBinding.btnFavorite.setFocusable(true);
        exitDialogBinding.btnCancel.setFocusable(true);
        exitDialogBinding.btnStartToggle.setFocusable(true);
        
        // 更新收藏按钮状态
        updateFavoriteButtonInDialog();
        
        // 设置启动按钮文案（启动XX），不再显示上下提示文字
        String currentStartPage = ValueUtil.getString(this, "startPage", "main");
        if ("main".equals(currentStartPage)) {
            exitDialogBinding.btnStartToggle.setText("启动即电视直播");
        } else {
            exitDialogBinding.btnStartToggle.setText("启动即视频点播");
        }
        
        // 根据当前X5开关状态：已开启则隐藏按钮；未开启则显示“开启X5内核(会关闭应用)”
        try {
            if (x5Ok()) {
                exitDialogBinding.btnOpenX5.setVisibility(View.GONE);
            } else {
                exitDialogBinding.btnOpenX5.setVisibility(View.VISIBLE);
                exitDialogBinding.btnOpenX5.setText("开启X5内核(会关闭应用)");
            }
        } catch (Throwable ignore) {}

        
        // 默认焦点回到收藏按钮，避免按返回后焦点丢失
        exitDialogBinding.btnFavorite.post(() -> exitDialogBinding.btnFavorite.requestFocus());

        // 设置左侧二维码图片
        try {
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(getAssets().open("tv-web/img/myzsm.jpg"));
            exitDialogBinding.qrDonate.setImageBitmap(bmp);
        } catch (Throwable ignore) {}
    }
    
    /**
     * 更新对话框中收藏按钮的状态
     */
    private void updateFavoriteButtonInDialog() {
        if (currentLive != null && favoriteService != null) {
            if (favoriteService.isFavorite(currentLive.getUrl())) {
                exitDialogBinding.btnFavorite.setText("取消收藏");
            } else {
                exitDialogBinding.btnFavorite.setText("收藏当前频道");
            }
        }
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
        
        // 收藏按钮
        exitDialogBinding.btnFavorite.setOnClickListener(v -> {
            if (currentLive != null) {
                toggleFavorite(currentLive);
                // 更新按钮状态
                updateFavoriteButtonInDialog();
            }
        });
        
        // 取消按钮
        exitDialogBinding.btnCancel.setOnClickListener(v -> {
            hideExitDialog();
        });
        
        // 点击背景关闭对话框
        exitDialogBinding.dialogBackdrop.setOnClickListener(v -> {
            hideExitDialog();
        });
        
        // 启动首页切换按钮（仅按钮，点击后切换并更新文案）
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
    }

    protected static String  videoQualityData=null;
    private void setupHzListInExit(){
        // 构造模拟画质项
        List<HzItem> hzItems=new ArrayList<>();
        if(null!=videoQualityData){
            hzItems=JsonUtil.fromJson(videoQualityData,new TypeToken<List<HzItem>>(){}.getType());
        }
        BaseBindingAdapter hzAdapter = new BaseBindingAdapter<HzItem, ItemHzLiveBinding>(hzItems,R.layout.item_hz_live) {
            @Override
            public void doBindViewHolder(BaseViewHolder<ItemHzLiveBinding> holder, HzItem item) {
                holder.getBinding().setVariable(BR.item, item);
                holder.getBinding().setVariable(BR.itemPresenter, ItemPresenter);
            }
        };
        hzAdapter.setItemPresenter(new HzLiveBindPresenter());
        exitDialogBinding.hzListInExit.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,false));
        exitDialogBinding.hzListInExit.setAdapter(hzAdapter);
        // 所有画质项的上下焦点跳转绑定（上->启动按钮，下->收藏按钮）
        exitDialogBinding.hzListInExit.addOnChildAttachStateChangeListener(new androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                View btn = view.findViewById(R.id.hzItem);
                if (btn != null) {
                    if (btn.getId() == View.NO_ID) {
                        btn.setId(View.generateViewId());
                    }
                    btn.setNextFocusUpId(exitDialogBinding.btnStartToggle.getId());
                    btn.setNextFocusDownId(exitDialogBinding.btnFavorite.getId());
                }
            }
            @Override
            public void onChildViewDetachedFromWindow(View view) { }
        });
        // 布局完成后，连接焦点链路（不改变默认焦点）
        exitDialogBinding.hzListInExit.post(() -> {
            try {
                androidx.recyclerview.widget.RecyclerView.ViewHolder vh = exitDialogBinding.hzListInExit.findViewHolderForAdapterPosition(0);
                if (vh instanceof BaseViewHolder) {
                    ItemHzLiveBinding b = (ItemHzLiveBinding) ((BaseViewHolder<?>) vh).getBinding();
                    View first = b.hzItem;
                    if (first.getId() == View.NO_ID) {
                        first.setId(View.generateViewId());
                    }
                    // 上下焦点：启动按钮 -> 画质第一项 -> 收藏按钮
                    exitDialogBinding.btnStartToggle.setNextFocusDownId(first.getId());
                    exitDialogBinding.btnFavorite.setNextFocusUpId(first.getId());
                    first.setNextFocusUpId(exitDialogBinding.btnStartToggle.getId());
                    first.setNextFocusDownId(exitDialogBinding.btnFavorite.getId());
                    // 不改变默认焦点
                }
            } catch (Exception ignore) {}
        });
    }
    
    

    private void bind(){
        binding = DataBindingUtil.setContentView(this, R.layout.activity_live);
        //binding.setMenuTitleHandler(new BaseWebViewActivity.MenuTitleHandler());
        ViewGroup container = binding.webviewWrapper;
        container.addView(mWebView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
       // lWebView=binding.webView;
        //focusChange();
    }


    protected void initWebChromeClient() {
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                isMenuShow=false;
                String url= view.getUrl();
                try {
                    url=  URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }
                LogUtil.i(TAG,"onProgressChangedX"+url);
                Vod vod = UpdateService.getByUrl(url);
                if(null!=vod){
                    currentLive=vod;
                    binding.liveName.setText(currentLive.getName()+" "+newProgress+"%");
                }
                if(newProgress==100){
                    HistoryDaoX.updateChannel(thisContext,url);
                    handler.sendMessageDelayed (handler.obtainMessage(2, "noText"),1000);
                }
                LogUtil.i("WebChromeClient", "onProgressChanged, newProgress:" + newProgress + ", view:" + view);
            }
            @Override
            public void onShowCustomView(View view, IX5WebChromeClient.CustomViewCallback callback) {
                LogUtil.i("WebChromeClient","onShowCustomView");
                binding.fullscreen.addView(view);
                binding.fullscreen.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                LogUtil.i("WebChromeClient","onPermissionRequest "+request.getOrigin());
                LogUtil.i("WebChromeClient",request.getOrigin()+" "+ Arrays.toString(request.getResources()));
                request.deny();
            }
            @Override
            public void onHideCustomView() {
                LogUtil.i("WebChromeClient","onHideCustomView");
                binding.fullscreen.removeAllViews();
                binding.fullscreen.setVisibility(View.GONE);
            }
        });
        mWebView.setWebChromeClientExtension(new X5WebChromeClientExtension());
    }

    @Override
    protected void webviewSet(IX5WebSettingsExtension webSettingsExtension) {
        //无图
        webSettingsExtension.setPicModel(IX5WebSettingsExtension.PicModel_NoPic);
    }

    @Override
    protected Object getJsInterface() {
        return new JsInterface();
    }

    private static boolean isMenuShow=false;
    public class JsInterface{

        // Android 调用 Js 方法1 中的返回值
        @JavascriptInterface
        public void toast(String message){
            LogUtil.i(TAG,"message "+message);
            ToastUtils.show(MyApplication.getContext(),message, Toast.LENGTH_SHORT);
        }
        @JavascriptInterface
        public void message(String service,String data){
            LogUtil.i(TAG,"service "+service+" data "+data);
            if("history.save".equals(service)){
                //final AppDatabase db = AppDatabase.getInstance(this);
                HistoryDaoX.save(thisContext, data, new StringCallback() {
                    @Override
                    public void data(String data) {
                        runOnUiThread(()->{
                            mWebView.loadUrl(data);
                        });
                    }
                });
                return;
            }
            if("history.update".equals(service)){
                HistoryDaoX.update(thisContext,data);
                return;
            }
            if("menuShow".equals(service)){
                //Util.evalOnUi(lWebView,data);
                if(data.equals("1")){
                    isMenuShow =true;
                }else{
                    isMenuShow =false;
                }
                return;
            }
            if("js".equals(service)){
                Util.evalOnUi(mWebView,data);
                return;
            }
            if("key".equals(service)){
                keyCodeAllByCode(data);
                return;
            }
            if("keyNum".equals(service)){
                keyEventAll(Integer.parseInt(data));
                return;
            }
            if("videoQuality".equals(service)){
                videoQualityData=data;
                return;
            }
        }
        @JavascriptInterface
        public String postJson(String url,String header, String requestBody){

            Map<String, String> headerMap= JsonUtil.fromJson(header,
                    new TypeToken<Map<String, String>>() {}.getType());
            if(!url.startsWith("http")){
                return FileUtil.readExt(MyApplication.getAppContext(),"tv-web/"+url);
            }
            LogUtil.i(TAG,headerMap.toString()+"url "+url+" "+requestBody);
            return HttpUtil.postJson(url,headerMap,requestBody);
        }
        @JavascriptInterface
        public String getJson(String url,String header){
            Map<String, String> headerMap= JsonUtil.fromJson(header,
                    new TypeToken<Map<String, String>>() {}.getType());
            if(!url.startsWith("http")){
                return FileUtil.readExt(MyApplication.getAppContext(),"tv-web/"+url);
            }
            LogUtil.i(TAG,headerMap.toString()+"url "+url);
            return HttpUtil.getJson(url,headerMap);
        }
        @JavascriptInterface
        public String getHtml(String url,String header){
            Map<String, String> headerMap= JsonUtil.fromJson(header,
                    new TypeToken<Map<String, String>>() {}.getType());
            LogUtil.i(TAG,headerMap.toString()+" getHtml "+url);
            return HttpUtil.getJson(url,headerMap);
        }

    }

    private void initData() {
        // 初始化收藏服务
        favoriteService = FavoriteService.getInstance(this);
        
        // 使用异步任务加载数据
        new Thread(() -> {
            // 在后台线程执行耗时操作
            List<Live> result = UpdateService.getByLivesWithFavorites(this);
            
            // 在UI线程更新界面
            runOnUiThread(() -> {
                provinces = result;
                currentProvinceIndex = currentLive.getTagIndex();
                showCurrentProvince();
            });
        }).start();
    }

    private Vod createVod(String name, String key, String url) {
        Vod vod = new Vod();
        vod.setName(name);
        vod.setKey(key);
        vod.setUrl(url);
        return vod;
    }

    private void showCurrentProvince() {
        if (provinces == null || provinces.isEmpty()) {
            // 处理空数据情况
            binding.provinceName.setText("无数据");
            setupChannelList(new ArrayList<>());
            return;
        }
        
        // 确保索引在有效范围内
        if (currentProvinceIndex < 0) {
            currentProvinceIndex = 0;
        } else if (currentProvinceIndex >= provinces.size()) {
            currentProvinceIndex = provinces.size() - 1;
        }
        
        Live currentProvince = provinces.get(currentProvinceIndex);
        int count = currentProvince.getVods() == null ? 0 : currentProvince.getVods().size();
        binding.provinceName.setText(currentProvince.getName() + "(" + count + ")");
        setupChannelList(currentProvince.getVods());
        // 显示后，将焦点与选中项指向当前频道
        try {
            if (currentLive != null && currentLive.getTagIndex() == currentProvinceIndex) {
                int idx = Math.max(0, currentLive.getDetailIndex());
                if (binding.channelList.getAdapter() != null && binding.channelList.getCount() > 0) {
                    if (idx >= binding.channelList.getCount()) { idx = binding.channelList.getCount() - 1; }
                    final int finalIdx = idx;
                    binding.channelList.post(() -> {
                        binding.channelList.setSelection(finalIdx);
                        binding.channelList.requestFocus();
                    });
                }
            }
        } catch (Throwable ignore) {}
    }

    private void setupChannelList(List<Vod> channels) {
        ArrayAdapter<Vod> adapter = new ArrayAdapter<Vod>(this, android.R.layout.simple_list_item_1, channels) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                Button btn;
                if (convertView == null) {
                    btn = new Button(getContext());
                    btn.setLayoutParams(new AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    btn.setTextColor(Color.WHITE);
                    btn.setTextSize(16);
                    btn.setPadding(24, 16, 24, 16);
                    btn.setBackgroundResource(R.drawable.menu_button_background);
                    btn.setClickable(false);
                    btn.setFocusable(false);
                } else {
                    btn = (Button) convertView;
                    
                    if (!(btn.getLayoutParams() instanceof AbsListView.LayoutParams)) {
                        btn.setLayoutParams(new AbsListView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                }
                
                Vod channel = getItem(position);
                if (channel != null) {
                    btn.setText(channel.getName());
                }
                
                return btn;
            }
        };
        binding.channelList.setAdapter(adapter);
        binding.channelList.setOnItemClickListener((parent, view, position, id) -> {
            try {
                Vod channel = channels.get(position);
                if (channel.getUrl() != null) {
                    currentLive = channel;
                    // 在主线程中执行WebView操作
                    runOnUiThread(() -> {
                        try {
                            LogUtil.i(TAG, "Loading URL in WebView: " + channel.getUrl());
                            mWebView.loadUrl(channel.getUrl());
                            LogUtil.i(TAG, "URL loaded successfully");
                        } catch (Exception e) {
                            LogUtil.e(TAG, "Error loading URL in WebView: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                    
                    // 更新历史记录
                    HistoryDaoX.updateChannel(thisContext, channel.getUrl());
                    
                    // 显示提示
                    showToast(channel.getName(), this);
                    
                    // 隐藏菜单
                    hideMenu();
                } else {
                    LogUtil.e(TAG, "Channel or URL is null");
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "Error handling channel click: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void showMenu() {
        binding.menuContainer.setVisibility(View.VISIBLE);
        isMenuShow = true;
        // 将省份定位到当前播放的省份
        try {
            if (currentLive != null) {
                currentProvinceIndex = currentLive.getTagIndex();
            }
        } catch (Throwable ignore) {}
        showCurrentProvince();
        setupProvinceButtons();
        // 将列表焦点与选中项定位到当前频道
        try {
            int sel = 0;
            if (currentLive != null) { sel = Math.max(0, currentLive.getDetailIndex()); }
            if (binding.channelList.getAdapter() != null && binding.channelList.getCount() > 0) {
                if (sel >= binding.channelList.getCount()) { sel = binding.channelList.getCount() - 1; }
                binding.channelList.setSelection(sel);
                binding.channelList.requestFocus();
            }
        } catch (Throwable ignore) {}
        
        // 点击空白处关闭菜单
        binding.menuContainer.setOnClickListener(v -> hideMenu());
    }

    private void setupProvinceButtons() {
        binding.prevProvinceArea.setOnClickListener(v -> {
            currentProvinceIndex--;
            if (currentProvinceIndex < 0) {
                currentProvinceIndex = provinces.size() - 1;
            }
            showCurrentProvince();
        });

        binding.nextProvinceArea.setOnClickListener(v -> {
            currentProvinceIndex++;
            if (currentProvinceIndex >= provinces.size()) {
                currentProvinceIndex = 0;
            }
            showCurrentProvince();
        });
    }

    private void hideMenu() {
        binding.menuContainer.setVisibility(View.GONE);
        isMenuShow = false;
        binding.menuContainer.setOnClickListener(null);
    }

    // 已移除悬浮画质层逻辑

    public class HzLiveBindPresenter implements tv.utao.x5.impl.IBaseBindingPresenter {
        public void onClick(HzItem item){
            if(item.getAction()!=null&&item.getAction().trim().length()>0){
                Util.evalOnUi(mWebView,item.getAction());
            }else if(item.getId()!=null){
                String js = "$$(\\\"#"+item.getId()+"\\\").click()";
                Util.evalOnUi(mWebView,js);
            }
        }
    }
    
    /**
     * 切换收藏状态
     * @param vod 要切换收藏状态的频道
     */
    private void toggleFavorite(Vod vod) {
        if (favoriteService.isFavorite(vod.getUrl())) {
            // 已收藏，取消收藏
            favoriteService.removeFavorite(vod.getUrl());
            ToastUtils.show(this, "已取消收藏: " + vod.getName(), Toast.LENGTH_SHORT);
        } else {
            // 未收藏，添加收藏
            favoriteService.addFavorite(vod);
            ToastUtils.show(this, "已收藏: " + vod.getName(), Toast.LENGTH_SHORT);
        }
        
        // 重新加载数据以更新界面
        initData();
    }


}