package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Killua Zoldyck on 2018/3/7.
 */

public class Weather {

    //监控是否返回成功
    public String status;

    public Basic basic;
    public AQI aqi;
    public NOW now;
    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}
