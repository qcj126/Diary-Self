package diary.notify.handler.exception;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 异常处理 Handler
 * 作为 Pipeline 中的最后一道防线，捕获前置 Handler 中未处理的异常
 *
 * Pipeline 位置：
 *   必须放在 Pipeline 的最后面（addLast），确保所有 Handler 抛出的异常都能被捕获
 *
 * 处理策略：
 *   1. 记录异常日志（包含连接信息、异常类型、异常消息）
 *   2. 根据异常类型决定是否需要关闭连接
 *   3. 防止异常传播到 Netty 底层导致整个服务崩溃
 */
@Slf4j
@Component
public class ExceptionHandler extends ChannelInboundHandlerAdapter {

    /**
     * 捕获前置 Handler 中抛出的异常
     * 当 Pipeline 中任何 Handler 调用 ctx.fireExceptionCaught() 或直接抛出异常时，
     * 此方法会被调用
     *
     * @param ctx   ChannelHandlerContext
     * @param cause 异常信息
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO: 第一步：记录异常日志
        //   - 记录 Channel 的唯一标识：ctx.channel().id()
        //   - 记录远程客户端地址：ctx.channel().remoteAddress()
        //   - 记录异常类型和消息：cause.getClass().getSimpleName(), cause.getMessage()
        log.error("Pipeline异常: channelId={}, remoteAddr={}, errorType={}, errorMsg={}",
                ctx.channel().id(),
                ctx.channel().remoteAddress(),
                cause.getClass().getSimpleName(),
                cause.getMessage());

        // TODO: 第二步：根据异常类型决定处理策略
        //   - 如果是 IOException（如连接断开）：属于正常情况，记录 DEBUG 级别日志即可
        //   - 如果是业务异常（如认证失败、消息格式错误）：记录 WARN 级别日志
        //   - 如果是未知异常：记录 ERROR 级别日志，并打印完整堆栈

        // TODO: 第三步：关闭连接
        //   - ctx.close()
        //   - 发生未知异常时，连接状态可能已不一致，关闭连接是最安全的做法
        //   - 客户端可自动重连

        // TODO: 第四步（可选）：发送错误消息给客户端
        //   - 在关闭连接前，向客户端发送一条错误通知
        //   - 例如：{"type": "ERROR", "content": "服务器内部错误"}
        //   - 让客户端知道连接断开的原因

        super.exceptionCaught(ctx, cause);
    }
}
