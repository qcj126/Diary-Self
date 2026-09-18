package com.executor.jobhandler;

import com.executor.service.WeatherService;
import com.executor.service.WeatherService.WeatherRefreshResult;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import diary.common.enums.xxljobenums.QWeatherLocationEnum;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class WeatherRefreshJob {

    private final WeatherService weatherService;

    public WeatherRefreshJob(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @XxlJob("weatherRefreshHandler")
    public void weatherRefreshHandler() {
        List<String> cities = Arrays.stream(QWeatherLocationEnum.values())
                .map(QWeatherLocationEnum::getName)
                .toList();
        XxlJobHelper.log("开始刷新天气缓存，城市数: " + cities.size());

        WeatherRefreshResult result = weatherService.refreshWeather(cities);
        String message = String.format("天气缓存刷新完成，总数: %d，成功: %d，失败: %d",
                result.totalCount(), result.successCount(), result.failedCount());
        XxlJobHelper.log(message);

        if (result.failedCount() > 0) {
            XxlJobHelper.handleFail(message);
        }
    }
}
