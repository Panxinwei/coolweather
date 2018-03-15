package com.example.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.coolweather.gson.Weather;
import com.example.coolweather.service.AutoUpdateService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //判断缓存数据 ,可以不用显示城市
       SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
     if(prefs.getString("weather",null)!=null)
        {
           Intent intent=new Intent(this, WeatherActivity.class);
           startActivity(intent);

           finish();
       }
    }
}
