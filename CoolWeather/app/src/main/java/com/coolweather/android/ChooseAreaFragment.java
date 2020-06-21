package com.coolweather.android;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private  TextView mTime;
    private Button backButton ;
    private ListView listView;
    private ArrayAdapter<String> adapter ;
    private List<String> dataList = new ArrayList<>();
    private static final int msgKey1 = 1;

    /**
 *省列表
 */
    private List<Province> provinceList ;
    /**
     *市列表
     */
    private List<City> cityList;
    /**
     *县列表
     */
    private List<County> countyList;
    /**
     *选中的省份
     */
     private Province selectedProvince;
     /**
     *选中的城市
     */
    private City selectedCity;
    /**
     当前选中的级别
     */
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        mTime = view.findViewById(R.id.mTime);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // 设置 ListView 和 Button 的点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    if(getActivity() instanceof WeatherActivity){   // 判断碎片的位置
                        //该碎片在WeatherActivity中，只需要刷新该活动
                        WeatherActivity activity = (WeatherActivity)getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }else if(getActivity() instanceof MainActivity){
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);    // 向intent传入WeatherId
                        startActivity(intent);
                        getActivity().finish();
                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });


        queryProvinces();
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
        String mMinute =
                String.valueOf(c.get(Calendar.MINUTE));//分
        String mSecond =
                String.valueOf(c.get(Calendar.SECOND));//秒

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
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }
        else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0){
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }
        else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0){
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }
        else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    private void queryFromServer(String address, final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            String responseText = response.body().string();
            boolean result = false;
            if ("province".equals(type)){
                result = Utility.handleProvinceResponse(responseText);
            }else if ("city".equals(type)){
                result = Utility.handleCityResponse(responseText, selectedProvince.getId());
            }else if ("county".equals(type)){
                result = Utility.handleCountyResponse(responseText, selectedCity.getId());
            }
            if (result){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        if ("province".equals(type)){
                            queryProvinces();
                        }else if ("city".equals(type)){
                            queryCities();
                        }else if ("county".equals(type)){
                            queryCounties();
                        }
                    }
                });
            }

            }
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }

    private  void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }

}
