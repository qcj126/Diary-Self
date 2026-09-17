package com.executor.service;

import com.executor.consts.CityLocationInfos;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import diary.common.entity.xxlJob.WeatherInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WeatherService {

    private static final String WEATHER_CACHE_PREFIX = "weather:";
    private static final Gson GSON = new Gson();
    private static final Map<String, MockWeather> MOCK_WEATHER = Map.of(
            "北京", new MockWeather("晴", "28", "45", "30", "东南风", "3"),
            "上海", new MockWeather("多云", "26", "68", "28", "东风", "2"),
            "广州", new MockWeather("阵雨", "30", "82", "33", "南风", "2"),
            "深圳", new MockWeather("多云转晴", "29", "75", "32", "西南风", "3"),
            "杭州", new MockWeather("阴", "24", "60", "25", "北风", "2"),
            "重庆", new MockWeather("小雨", "20", "85", "19", "东北风", "1")
    );

    private final StringRedisTemplate redisTemplate;
    private final OkHttpClient httpClient;
    private final String apiHost;
    private final String apiKey;
    private final Duration cacheTtl;

    @Autowired
    public WeatherService(StringRedisTemplate redisTemplate,
                          @Value("${weather.qweather.api-host:https://n37h2tkmw4.re.qweatherapi.com}") String apiHost,
                          @Value("${weather.qweather.api-key:}") String apiKey,
                          @Value("${weather.cache-ttl-hours:7}") long cacheTtlHours) {
        this(redisTemplate, new OkHttpClient(), apiHost, apiKey, cacheTtlHours);
    }

    WeatherService(StringRedisTemplate redisTemplate, OkHttpClient httpClient,
                   String apiHost, String apiKey, long cacheTtlHours) {
        this.redisTemplate = redisTemplate;
        this.httpClient = httpClient;
        this.apiHost = removeTrailingSlash(apiHost);
        this.apiKey = apiKey;
        this.cacheTtl = Duration.ofHours(Math.max(cacheTtlHours, 1));
    }

    public WeatherInfo getWeather(String city) {
        requireSupportedCity(city);

        WeatherInfo cached = readCache(city);
        if (cached != null) {
            return cached;
        }

        try {
            WeatherInfo weather = callWeatherApi(city);
            writeCache(city, weather);
            return weather;
        } catch (Exception exception) {
            log.error("获取天气失败，使用临时降级数据，city: {}", city, exception);
            return fallbackWeather(city);
        }
    }

    /**
     * 主动刷新指定城市的天气缓存。刷新失败时不会覆盖原有缓存。
     */
    public WeatherRefreshResult refreshWeather(Collection<String> cities) {
        if (cities == null || cities.isEmpty()) {
            return new WeatherRefreshResult(0, 0, 0);
        }

        LinkedHashSet<String> uniqueCities = cities.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int successCount = 0;
        int failedCount = 0;

        for (String city : uniqueCities) {
            try {
                requireSupportedCity(city);
                WeatherInfo weather = callWeatherApi(city);
                writeCache(city, weather);
                successCount++;
            } catch (Exception exception) {
                failedCount++;
                log.error("刷新天气缓存失败，city: {}", city, exception);
            }
        }
        return new WeatherRefreshResult(uniqueCities.size(), successCount, failedCount);
    }

    private WeatherInfo readCache(String city) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey(city));
            return StringUtils.hasText(json) ? GSON.fromJson(json, WeatherInfo.class) : null;
        } catch (Exception exception) {
            log.warn("读取天气缓存失败，将直接查询天气接口，city: {}", city, exception);
            return null;
        }
    }

    private void writeCache(String city, WeatherInfo weather) {
        redisTemplate.opsForValue().set(cacheKey(city), GSON.toJson(weather), cacheTtl);
    }

    private WeatherInfo callWeatherApi(String city) {
        Integer locationId = requireSupportedCity(city);
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("weather.qweather.api-key 未配置");
        }

        String url = apiHost + "/v7/weather/now?location=" + locationId;
        Request request = new Request.Builder()
                .url(url)
                .header("X-QW-Api-Key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("和风天气 API 调用失败，HTTP 状态码: " + response.code());
            }

            JsonObject root = GSON.fromJson(body.string(), JsonObject.class);
            if (root == null || !root.has("code") || !"200".equals(root.get("code").getAsString())
                    || !root.has("now")) {
                String responseCode = root != null && root.has("code")
                        ? root.get("code").getAsString() : "unknown";
                throw new IllegalStateException("和风天气 API 返回异常，业务状态码: " + responseCode);
            }

            JsonObject nowJson = root.getAsJsonObject("now");
            if (root.has("fxLink")) {
                nowJson.addProperty("fxLink", root.get("fxLink").getAsString());
            }
            return GSON.fromJson(nowJson, WeatherInfo.class);
        } catch (Exception exception) {
            throw new IllegalStateException("调用和风天气 API 异常，city: " + city, exception);
        }
    }

    private WeatherInfo fallbackWeather(String city) {
        MockWeather mock = MOCK_WEATHER.getOrDefault(city,
                new MockWeather("晴", "25", "55", "26", "微风", "1"));
        WeatherInfo info = new WeatherInfo();
        info.setObsTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        info.setText(mock.condition());
        info.setTemp(mock.temp());
        info.setFeelsLike(mock.feelsLike());
        info.setHumidity(mock.humidity());
        info.setWindDir(mock.windDirection());
        info.setWindScale(mock.windScale());
        return info;
    }

    private Integer requireSupportedCity(String city) {
        if (!StringUtils.hasText(city)) {
            throw new IllegalArgumentException("city 不能为空");
        }
        Integer locationId = CityLocationInfos.fromName(city.trim());
        if (locationId == null) {
            throw new IllegalArgumentException("未配置城市编码，city: " + city);
        }
        return locationId;
    }

    private static String cacheKey(String city) {
        return WEATHER_CACHE_PREFIX + city;
    }

    private static String removeTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("weather.qweather.api-host 不能为空");
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record WeatherRefreshResult(int totalCount, int successCount, int failedCount) {
    }

    private record MockWeather(String condition, String temp, String humidity, String feelsLike,
                               String windDirection, String windScale) {
    }
}
