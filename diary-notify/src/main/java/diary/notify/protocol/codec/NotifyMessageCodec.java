package diary.notify.protocol.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.notify.protocol.message.NotifyMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通知消息编解码器
 * 继承 MessageToMessageCodec，实现 NotifyMessage 与 TextWebSocketFrame 之间的双向转换
 *
 * 编码方向（出站）：NotifyMessage → TextWebSocketFrame
 *   - 将业务消息对象序列化为 JSON 字符串
 *   - 包装为 TextWebSocketFrame 通过 WebSocket 发送
 *
 * 解码方向（入站）：TextWebSocketFrame → NotifyMessage
 *   - 从 WebSocket 帧中提取 JSON 文本
 *   - 反序列化为 NotifyMessage 对象供 Handler 处理
 *
 * Pipeline 位置：
 *   位于 HeartbeatHandler 之后、NotifyHandler 之前
 *   确保消息在业务处理前已完成编解码
 */
@Slf4j
@Component
public class NotifyMessageCodec extends MessageToMessageCodec<TextWebSocketFrame, NotifyMessage> {

    // TODO: 注入或创建 ObjectMapper 实例（用于 JSON 序列化/反序列化）
    //   - 可复用 Spring 容器中的 ObjectMapper
    //   - 也可 new ObjectMapper() 独立使用
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 编码方法（出站方向）
     * 将 NotifyMessage 编码为 TextWebSocketFrame，发送到客户端
     *
     * @param ctx ChannelHandlerContext
     * @param msg 待编码的 NotifyMessage 消息
     * @param out 编码结果输出列表
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, NotifyMessage msg, List<Object> out) throws Exception {
        // TODO: 第一步：使用 objectMapper.writeValueAsString(msg) 将 NotifyMessage 序列化为 JSON 字符串
        String json = objectMapper.writeValueAsString(msg);

        // TODO: 第二步：将 JSON 字符串包装为 TextWebSocketFrame 并添加到输出列表
        out.add(new TextWebSocketFrame(json));

        // TODO: 第三步：记录调试日志，包含消息类型（可选）
        log.debug("编码消息: notifyType={}", msg.getNotifyType());
    }

    /**
     * 解码方法（入站方向）
     * 将客户端发送的 TextWebSocketFrame 解码为 NotifyMessage
     *
     * @param ctx   ChannelHandlerContext
     * @param frame 客户端发送的 WebSocket 文本帧
     * @param out   解码结果输出列表
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, List<Object> out) throws Exception {
        // TODO: 第一步：从 TextWebSocketFrame 中提取 JSON 文本
        //   - 调用 frame.text() 获取文本内容
        String json = frame.text();

        // TODO: 第二步：使用 objectMapper.readValue(json, NotifyMessage.class) 反序列化为 NotifyMessage 对象
        NotifyMessage message = objectMapper.readValue(json, NotifyMessage.class);

        // TODO: 第三步：将解码后的 NotifyMessage 添加到输出列表，传递给下一个 Handler
        out.add(message);

        // TODO: 第四步：记录调试日志，包含消息类型（可选）
        log.debug("解码消息: notifyType={}", message.getNotifyType());
    }
}
