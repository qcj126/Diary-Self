package com.xxl.job.executor.entity.vo;

import lombok.Data;

@Data
public class WeatherInfo {
    private String obsTime;     // 观测时间
    private String temp;        // 温度 ℃
    private String feelsLike;   // 体感温度 ℃
    private String icon;        // 天气图标代码
    private String text;        // 天气状况文字（晴、多云等）
    private String wind360;     // 风向角度
    private String windDir;     // 风向
    private String windScale;   // 风力等级
    private String windSpeed;   // 风速 km/h
    private String humidity;    // 湿度 %
    private String precip;      // 降水量 mm
    private String pressure;    // 大气压强 hPa
    private String vis;         // 能见度 km
    private String cloud;       // 云量
    private String dew;         // 露点温度 ℃
    private String fxLink;      // 天气预报链接
}
