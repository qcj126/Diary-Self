package com.executor.service;

import com.executor.consts.CityLocationInfos;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import diary.common.entity.xxlJob.WeatherInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final Gson gson = new Gson();
    private static final String QWEATHER_API_KEY = "866cf7e17b1c4909acf66b70b664db56";
    private static final OkHttpClient httpClient = new OkHttpClient();

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 模拟天气数据（无真实 API Key 时使用）
     */
    private static final Map<String, WeatherInfo> MOCK_WEATHER_MAP = new HashMap<>();

    static {
        MOCK_WEATHER_MAP.put("北京", buildMockWeather("北京", "晴", "28", "45", "30", "东南风", "3级"));
        MOCK_WEATHER_MAP.put("上海", buildMockWeather("上海", "多云", "26", "68", "28", "东风", "2级"));
        MOCK_WEATHER_MAP.put("广州", buildMockWeather("广州", "阵雨", "30", "82", "33", "南风", "2级"));
        MOCK_WEATHER_MAP.put("深圳", buildMockWeather("深圳", "多云转晴", "29", "75", "32", "西南风", "3级"));
        MOCK_WEATHER_MAP.put("杭州", buildMockWeather("杭州", "阴", "24", "60", "25", "北风", "2级"));
        MOCK_WEATHER_MAP.put("重庆", buildMockWeather("重庆", "小雨", "20", "85", "19", "东北风", "1级"));
    }

    private static WeatherInfo buildMockWeather(String city, String condition, String temp,
                                                String humidity, String feelsLike,
                                                String windDirection, String windPower) {
        WeatherInfo info = new WeatherInfo();
        info.setObsTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm+08:00")));
        info.setText(condition);
        info.setTemp(temp);
        info.setFeelsLike(feelsLike);
        info.setHumidity(humidity);
        info.setWindDir(windDirection);
        info.setWindScale(windPower);
        return info;
    }

    public WeatherInfo getWeather(String city) {
        // 尝试从 Redis 缓存读取
        if (redisTemplate != null) {
//            String cacheKey = "weather:" + city;
//            String cached = redisTemplate.opsForValue().get(cacheKey);
//            if (cached != null) {
//                return gson.fromJson(cached, WeatherInfo.class);
//            }

            WeatherInfo weather = callWeatherApi(city);
//            redisTemplate.opsForValue().set(cacheKey, gson.toJson(weather), 30, TimeUnit.MINUTES);
            return weather;
        }

        // Redis 不可用时直接返回
        return callWeatherApi(city);
    }

    /**
     * 调用和风天气 API 获取实时空气质量
     */
    private WeatherInfo callWeatherApi(String city) {
        Integer locationId = CityLocationInfos.fromName(city);
        if (locationId == null) {
            logger.warn("未找到城市编码，使用模拟数据：city={}", city);
            return MOCK_WEATHER_MAP.getOrDefault(city,
                    buildMockWeather(city, "晴", "25", "55", "26", "微风", "1级"));
        }

        String url = "https://n37h2tkmw4.re.qweatherapi.com/v7/weather/now?location="
                + locationId;

        Request request = new Request.Builder()
                .url(url)
                .header("X-QW-Api-Key", QWEATHER_API_KEY)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    String json = body.string();
                    logger.info("和风天气 API 响应：city={}, code={}", city, response.code());
                    logger.debug("响应体：{}", json);
                    JsonObject root = gson.fromJson(json, JsonObject.class);
                    JsonObject nowJson = root.getAsJsonObject("now");
                    String fxLink = root.get("fxLink").getAsString();
                    nowJson.addProperty("fxLink", fxLink);
                    return gson.fromJson(nowJson, WeatherInfo.class);
                }
            }
            throw new RuntimeException("和风天气 API 调用失败，响应码：" + response.code());
        } catch (Exception e) {
            logger.error("调用和风天气 API 异常：city={}", city, e);
        }

        // API 调用失败时降级为模拟数据
        return MOCK_WEATHER_MAP.getOrDefault(city,
                buildMockWeather(city, "晴", "25", "55", "26", "微风", "1级"));
    }
}