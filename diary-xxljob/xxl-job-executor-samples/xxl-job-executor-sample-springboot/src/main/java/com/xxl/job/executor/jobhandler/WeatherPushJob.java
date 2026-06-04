package com.xxl.job.executor.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.entity.UserPushConfig;
import com.xxl.job.executor.entity.vo.WeatherInfo;
import com.xxl.job.executor.mapper.UserMapper;
import com.xxl.job.executor.service.PushService;
import com.xxl.job.executor.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 天气推送任务
 * 支持分片广播模式，多执行器并发处理不同用户分片
 */
@Component
public class WeatherPushJob {

    private static final Logger log = LoggerFactory.getLogger(WeatherPushJob.class);

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private PushService pushService;

    @Autowired
    private UserMapper userMapper;

    @XxlJob("weatherPushHandler")
    public void weatherPushHandler() throws Exception {
        // 获取分片参数（如果使用分片广播模式）
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        log.info("天气推送任务开始执行，分片参数：{}/{}", shardIndex, shardTotal);

        // 1. 获取需要推送的用户列表（支持分片处理）
        List<UserPushConfig> users = userMapper.selectByShard(shardTotal, shardIndex);

        // 2. 批量获取天气数据（按城市聚合，减少API调用）
        Map<String, WeatherInfo> weatherCache = new HashMap<>();

        for (UserPushConfig user : users) {
            try {
                // 获取天气数据（带缓存）
                WeatherInfo weather = weatherCache.computeIfAbsent(
                        user.getCity(),
                        city -> weatherService.getWeather(city)
                );

                // 组装消息
                String message = buildWeatherMessage(user, weather);

                // 推送消息
                pushService.push(user.getPushType(), user.getTargetId(), message);

                // 记录日志
                XxlJobHelper.log("推送成功: {} -> {}", user.getTargetId(), user.getCity());

            } catch (Exception e) {
                log.error("推送失败: {}", user.getTargetId(), e);
                XxlJobHelper.log("推送失败: " + e.getMessage());
            }
        }

        log.info("天气推送任务执行完成，共推送{}人", users.size());
    }

    private String buildWeatherMessage(UserPushConfig user, WeatherInfo weather) {
        return String.format(
                "🌤️ 早安 %s！\n\n今日天气：%s\n当前温度：%s℃\n体感温度：%s℃\n湿度：%s%%\n风向：%s %s级\n\n%s",
                user.getNickname(),
                weather.getText(),
                weather.getTemp(),
                weather.getFeelsLike(),
                weather.getHumidity(),
                weather.getWindDir(),
                weather.getWindScale(),
                getDailyTip(weather.getText())
        );
    }

    private String getDailyTip(String condition) {
        if (condition.contains("雨")) return "☔ 出门记得带伞哦~";
        if (condition.contains("晴")) return "☀️ 天气晴好，注意防晒！";
        return "🏠 天气舒适，适合出门活动~";
    }
}
