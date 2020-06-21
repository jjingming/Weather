package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;

    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    private static final int msgKey1 = 1;
    public SwipeRefreshLayout swipeRefreshLayout;
    private String weatherId;
    private  TextView mTime;

    public DrawerLayout drawerLayout;
    private Button navButton;
    private Button switchButton1;
    private Button switchButton2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();    // 获取DecorView
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            |View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );  // 改变系统UI
            getWindow().setStatusBarColor(Color.TRANSPARENT);   // 设置透明
        }

        setContentView(R.layout.activity_weather);
        switchButton1 = (Button) findViewById(R.id.btn_switch1);
        switchButton2 = (Button) findViewById(R.id.btn_switch2);
        mTime = findViewById(R.id.mTime);
        //初始化各组件
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        bingPicImg = findViewById(R.id.bing_pic_img);

        switchButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bingPicImg.setImageResource(R.drawable.a1);
            }
        });


        switchButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bingPicImg.setImageResource(R.drawable.a2);
            }
        });
        //初始化各组件
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);

        weatherId = getIntent().getStringExtra("weather_id");
        if (weatherId!=null){
           // weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
//        else {
//            if (weatherString != null) {
//                //有缓存时直接解析天气数据
//                Weather weather = Utility.handleWeatherResponse(weatherString);
//                weatherId = weather.basic.weatherId;
//                showWeatherInfo(weather);
//            } else {
//                //无缓存时去服务器查询数据
//                weatherLayout.setVisibility(View.INVISIBLE);
//                requestWeather(weatherId);
//            }
//        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {   // 设置下拉刷新监听器
                requestWeather(weatherId);
            }
        });

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             //   drawerLayout.openDrawer(GravityCompat.START);   // 打开滑动菜单
                Intent intent = new Intent(WeatherActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        new TimeThread().start();
    }


    public class TimeThread extends Thread {
        @Override

        public void run () {
            do {
                try {

                    Thread.sleep(1000);
                    Message msg = new Message();

                    msg.what = msgKey1;

                    mHandler.sendMessage(msg);
                }
                catch
                (InterruptedException e) {
                    e.printStackTrace();

                }
            } while(true);
        }
    }

    private Handler
            mHandler = new Handler() {
        @Override
        public void
        handleMessage (Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case msgKey1:
                    mTime.setText(getTime());
                    break;
                default:

                    break;
            }
        }
    };
    //获得当前年月日时分秒星期
    public String getTime(){
        final Calendar c =
                Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone
                ("GMT+8:00"));
        String mYear = String.valueOf(c.get
                (Calendar.YEAR)); // 获取当前年份
        String mMonth =
                String.valueOf(c.get(Calendar.MONTH) + 1);// 获取当前月份

        String mDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));// 获取
        //    当前月份的日期号码
        String mWay = String.valueOf(c.get
                (Calendar.DAY_OF_WEEK));
        String mHour = String.valueOf(c.get
                (Calendar.HOUR_OF_DAY));//时
        if(mHour.length() == 1)
            mHour = "0"+mHour;
        String mMinute =
                String.valueOf(c.get(Calendar.MINUTE));//分
        if(mMinute.length() == 1)
            mMinute = "0"+mMinute;
        String mSecond =
                String.valueOf(c.get(Calendar.SECOND));//秒
        if(mSecond.length() == 1)
            mSecond = "0"+mSecond;

        if("1".equals
                (mWay)){
            mWay ="天";
        }else if("2".equals(mWay)){

            mWay ="一";
        }else if("3".equals(mWay)){
            mWay
                    ="二";
        }else if("4".equals(mWay)){
            mWay ="三";

        }else if("5".equals(mWay)){
            mWay ="四";
        }else if
        ("6".equals(mWay)){
            mWay ="五";
        }else if("7".equals
                (mWay)){
            mWay ="六";
        }
        return mYear + "年"
                + mMonth + "月" + mDay+"日"+"  "+"星期"+mWay+"  "+mHour
                +":"+mMinute+":"+mSecond;
    }
    /**
     * 根据天气Id请求城市天气信息
     */
    public void requestWeather(final String weatherId){
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId
                + "&key=6ebfd087db8144cbaab3884bb8f4b19d"; // 这里的key设置为第一个实训中获取到的API Key
        // 组装地址并发出请求
        Log.i("cityid*******",weatherId);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final  Weather weather = Utility.handleWeatherResponse(responseText);   // 将返回数据转换为Weather对象
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null && "ok".equals(weather.status)){
                            //缓存有效的weather对象(实际上缓存的是字符串)
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);   // 显示内容
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);    // 表示刷新事件结束并隐藏刷新进度条
                    }
                });
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    /**
     * 处理并展示Weather实体类中的数据
     * @param weather
     */
    private void showWeatherInfo(Weather weather){

        // 从Weather对象中获取数据
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1]; //按24小时计时的时间
        String degree = weather.now.temperature + "°C";
        String weatherInfo = weather.now.more.info;

        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);

        // 将数据显示到对应控件上
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();

        for(Forecast forecast:weather.forecastList){    // 循环处理每天的天气信息
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);

            // 加载布局
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);

            // 设置数据
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);

            // 添加到父布局
            forecastLayout.addView(view);
        }
        if(weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度: " + weather.suggestion.comfort.info;
        String carWash = "洗车指数: " + weather.suggestion.carWash.info;
        String sport = "运动建议: "+ weather.suggestion.sport.info;

        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);

        weatherLayout.setVisibility(View.VISIBLE);  // 将天气信息设置为可见
    }
    /**
     * 加载必应每日一图
     */
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bingPic = response.body().string();    // 获取背景图链接
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply(); // 将北京图链接存到 SharedPreferences 中
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);    // 用 Glide 加载图片
                    }
                });
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }
        });
    }
}