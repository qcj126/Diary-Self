package diary.notify.handler.auth;

import diary.utils.jwt.JwtUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 认证 Handler
 * 在 WebSocket 握手阶段验证用户身份
 * <p>
 * 认证流程：
 * 1. 客户端通过 WebSocket 连接，URL 中携带 token 参数
 * 例如：ws://host:port/ws?token=xxx
 * 2. 服务端在握手阶段拦截请求，提取 token
 * 3. 验证 token 的合法性（签名、有效期等）
 * 4. 验证通过：将 userId 存入 Channel 的 Attribute，放行后续 Handler
 * 5. 验证失败：返回 401 状态码，关闭连接
 * <p>
 * 注意事项：
 * - 此 Handler 必须在 WebSocketServerProtocolHandler 之前添加
 * - 或者监听 UserEventTriggered 事件，在握手完成后处理
 */
@Slf4j
@Component
public class JwtAuthHandler extends ChannelInboundHandlerAdapter {

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 处理用户事件
     * 当 WebSocket 握手完成后，WebSocketServerProtocolHandler 会触发 HandshakeComplete 事件
     *
     * @param ctx ChannelHandlerContext
     * @param evt 事件对象
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        //   - if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete)
        //   - 只有握手完成后，才能从请求头中获取 token
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            //   - 通过 ((HandshakeComplete) evt).requestHeaders() 获取请求头
            //   - token 可能在 URL 参数中（?token=xxx），也可能在 Header 中（Authorization: Bearer xxx）
            //   - 需要根据前端约定选择获取方式
            HttpHeaders httpHeaders = ((WebSocketServerProtocolHandler.HandshakeComplete) evt).requestHeaders();
            String token = httpHeaders.get("token");
            //   - 调用 jwtUtil.parseToken(token) 解析 token
            //   - 检查签名是否正确、是否过期、是否被篡改
            //   - 从 token 中提取 userId
            String username = jwtUtil.parseToken(token);
            //   - ctx.channel().attr(AttributeKey.valueOf("userId")).set(userId)
            //   - 后续 Handler 可通过 Attribute 获取当前连接的用户身份
            //   - 记录日志：认证成功，userId=xxx
            ctx.channel().attr(AttributeKey.valueOf("username")).set(username);
            //   - ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED))
            //   - ctx.close()
            //   - 记录日志：认证失败，原因=xxx
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED));
            ctx.close();
            log.error("JWT认证失败: channel={}, token={}", ctx.channel().id(), token);
        }
    }

    /**
     * 异常处理
     * 当认证过程中发生异常时，关闭连接并记录日志
     *
     * @param ctx   ChannelHandlerContext
     * @param cause 异常信息
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("JWT认证异常: channel={}, error={}", ctx.channel().id(), cause.getMessage());

        //   - ctx.close()
        //   - 防止未认证的连接继续发送消息
        ctx.close();
    }
}
