package com.example.coolweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Killua Zoldyck on 2018/3/7.
 */

public class ChooseAreaFragment extends Fragment {
   // private static final String TAG="ChooseAreaFragment";

    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;

    //进度对话框
    private ProgressDialog progressDialog;

    private TextView titleText;
    private Button backButton;

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    //当前选中
    private Province selectedProvince;
    private City selectedCity;

    //当前级别
    private int currentLevel;

    @Nullable
    @Override
    //创建View 并为listview设置adapter
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        //应用choose_area
        View view=inflater.inflate(R.layout.choose_area,container,false);

        titleText=(TextView) view.findViewById(R.id.title_text);
        backButton=(Button)view.findViewById(R.id.back_button);
        listView=(ListView)view.findViewById(R.id.list_view);

        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    //为listview设置点击事件
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //使用Lambd在里面只有一个方法需要实现时，把参数挪到上面来，用->  通常用在click /runable
        listView.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {

                if(currentLevel==LEVEL_PROVINCE)
                {
                    selectedProvince=provinceList.get(position);
                    queryCities();
                }
                else if(currentLevel==LEVEL_CITY)
                {
                    selectedCity=cityList.get(position);
                    queryCounties();

                }
                else if(currentLevel==LEVEL_COUNTY)
                {
                    //获取天气
                    String weatherId=countyList.get(position).getWeatherId();
                    //如果从MainActivity中选择
                    if(getActivity() instanceof  MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }
                    //如果从天气界面选择
                    else if(getActivity() instanceof WeatherActivity)
                    {
                        WeatherActivity weatherActivity= (WeatherActivity) getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefreshLayout.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
                    }
                }
            }
        );

        backButton.setOnClickListener(view ->  {

                if(currentLevel==LEVEL_COUNTY)
                {
                    queryCities();

                }
                else if(currentLevel==LEVEL_CITY)
                {
                    queryProvinces();
                }

            }
        );
        //最上层点返回键还是省份
        queryProvinces();
    }



    private void queryProvinces() {
        titleText.setText("中国");
        //能够回退
        backButton.setVisibility(View.GONE);
        //查找数据库
        provinceList= DataSupport.findAll(Province.class);
        if(provinceList.size()>0)
        {
            dataList.clear();
             //遍历provinceList 放入dataList
            for(Province province:provinceList)
            {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            //选中第一个
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;

        }
        else {
            //从服务器中查找
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }

    }

    private void queryCities() {

        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        //  封装的sql语句
        cityList=DataSupport.where("provinceid=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size()>0)
        {
            dataList.clear();
            for(City city:cityList)
            {
                dataList.add(city.getCityName());

            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;

        }
        else {
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address,"city");
        }
    }
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=DataSupport.where("cityid=?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0)
        {
            dataList.clear();
            for(County county:countyList)
            {
                dataList.add(county.getCountyName());

            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;

        }
        else {
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/" + provinceCode + "/"+ cityCode;
            queryFromServer(address,"county");

        }
    }

    //从服务器中查找
    private void queryFromServer(String address,final String type) {
    //显示进度对话框
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                getActivity().runOnUiThread(()-> {

                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
            );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type))
                {
                    //解析 传入数据库save 并返回true
                    result= Utility.handleProvinceResponse(responseText);

                }
                else if("city".equals(type))
                {
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());

                }
                else if("county".equals(type))
                {
                    result=Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if(result)
                {
                    getActivity().runOnUiThread(()-> {

                            closeProgressDialog();
                            if("province".equals(type))
                            {
                                queryProvinces();
                            }
                            else if("city".equals(type))
                            {
                                queryCities();
                            }
                            else if("county".equals(type))
                            {
                                queryCounties();
                            }
                        }
                    );
                }
            }
        });
    }



    private void showProgressDialog() {
        if(progressDialog==null)
        {
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
    if(progressDialog!=null)
    {
        progressDialog.dismiss();
    }

    }

}
