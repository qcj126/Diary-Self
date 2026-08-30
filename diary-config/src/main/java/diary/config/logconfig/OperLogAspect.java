package diary.config.logconfig;

import diary.common.entity.log.OperLogPO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.alibaba.fastjson.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Aspect
@Component
public class OperLogAspect {

    /**
     * 定义切点：拦截带有@OperationLog注解的方法
     */
    @Pointcut("@annotation(diary.config.logconfig.OperLog)")
    public void OperLog() {}

    /**
     * 环绕通知
     */
    @Around("OperLog()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取当前请求信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解信息
        OperLog operationLog = method.getAnnotation(OperLog.class);

        // 构建日志对象
        OperLogPO logEntity = new OperLogPO();
        logEntity.setModule(operationLog.module());
        logEntity.setDescription(operationLog.description());
        logEntity.setOperationType(operationLog.operationType());
        logEntity.setRequestUrl(request.getRequestURI());
        logEntity.setRequestMethod(request.getMethod());
        logEntity.setIp(getIpAddress(request));
        logEntity.setOperator(getCurrentUser()); // 获取当前操作用户
        logEntity.setCreateTime(LocalDateTime.now());

        // 保存请求参数
        if (operationLog.saveParams()) {
            Object[] args = joinPoint.getArgs();
            // 过滤掉不需要记录的参数
            String params = JSON.toJSONString(args);
            logEntity.setRequestParams(params);
        }

        Object result = null;
        try {
            // 执行目标方法
            result = joinPoint.proceed();

            // 保存返回结果
            if (operationLog.saveResult()) {
                logEntity.setResponseResult(JSON.toJSONString(result));
            }

            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;
            logEntity.setCostTime((int) costTime);

            // 保存日志
            saveLog(logEntity);

            return result;
        } catch (Exception e) {
            // 记录异常信息
            logEntity.setResponseResult("异常：" + e.getMessage());
            saveLog(logEntity);
            throw e;
        }
    }

    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取当前操作用户（根据实际情况实现）
     */
    private String getCurrentUser() {
        // 可以从SecurityContext、ThreadLocal等获取
        return "admin"; // 示例
    }

    /**
     * 保存日志到数据库
     */
    private void saveLog(OperLogPO logEntity) {
        // 注入Service保存到数据库
        // operationLogService.save(logEntity);
        log.info("操作日志：{}", JSON.toJSONString(logEntity));
    }
}
