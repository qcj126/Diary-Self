package com.executor.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息推送服务（模拟实现，实际可对接微信/邮件/短信等渠道）
 */
@Slf4j
@Service
public class PushService {

    /**
     * 推送消息
     *
     * @param pushType  推送类型（wechat / email / sms）
     * @param targetId  推送目标ID
     * @param message   消息内容
     */
    public void push(String pushType, String targetId, String message) {
        switch (pushType) {
            case "wechat":
                pushWechat(targetId, message);
                break;
            case "email":
                pushEmail(targetId, message);
                break;
            case "sms":
                pushSms(targetId, message);
                break;
            default:
                log.warn("未知的推送类型：{}，目标：{}", pushType, targetId);
        }
    }

    /**
     * 微信推送（模拟）
     */
    private void pushWechat(String openId, String message) {
        // TODO: 对接微信公众号模板消息接口
        log.info("[微信推送] openId={}, message={}", openId, message);
    }

    /**
     * 邮件推送（模拟）
     */
    private void pushEmail(String email, String message) {
        // TODO: 对接 JavaMailSender 发送邮件
        log.info("[邮件推送] email={}, message={}", email, message);
    }

    /**
     * 短信推送（模拟）
     */
    private void pushSms(String phone, String message) {
        // TODO: 对接短信服务商 API 发送短信
        log.info("[短信推送] phone={}, message={}", phone, message);
    }
}
