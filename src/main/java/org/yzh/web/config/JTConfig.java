package org.yzh.web.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yzh.framework.codec.Delimiter;
import org.yzh.framework.mvc.HandlerMapping;
import org.yzh.framework.mvc.SpringHandlerMapping;
import org.yzh.framework.netty.NettyConfig;
import org.yzh.framework.netty.TCPServer;
import org.yzh.framework.session.MessageManager;
import org.yzh.framework.session.SessionListener;
import org.yzh.framework.session.SessionManager;
import org.yzh.protocol.codec.JTMessageEncoder;
import org.yzh.protocol.codec.MultiPacketDecoder;
import org.yzh.web.component.adapter.JTMessageAdapter;
import org.yzh.web.endpoint.JTHandlerInterceptor;
import org.yzh.web.endpoint.JTMultiPacketListener;
import org.yzh.web.endpoint.JTSessionListener;

@Configuration
@ConditionalOnProperty(value = "tpc-server.jt808.enable", havingValue = "true")
public class JTConfig implements InitializingBean, DisposableBean {

    @Value("${tpc-server.jt808.port}")
    private int port;

    @Autowired
    private TCPServer jt808Server;

    @Bean
    public TCPServer jt808Server() {
        NettyConfig jtConfig = NettyConfig.custom()
                .setPort(port)
                .setMaxFrameLength(2 + 21 + 1023 + 2)
                .setDelimiters(new Delimiter(new byte[]{0x7e}))
                .setDecoder(messageAdapter())
                .setEncoder(messageAdapter())
                .setSessionManager(sessionManager())
                .setHandlerMapping(handlerMapping())
                .setHandlerInterceptor(handlerInterceptor())
                .build();
        return new TCPServer("808服务", jtConfig);
    }

    @Bean
    public JTMessageAdapter messageAdapter() {
        return new JTMessageAdapter(
                new JTMessageEncoder("org.yzh.protocol"),
                new MultiPacketDecoder("org.yzh.protocol", multiPacketListener())
        );
    }

    @Bean
    public JTMultiPacketListener multiPacketListener() {
        return new JTMultiPacketListener(10);
    }

    @Bean
    public MessageManager messageManager() {
        return new MessageManager(sessionManager());
    }

    @Bean
    public SessionManager sessionManager() {
        return new SessionManager(sessionListener());
    }

    @Bean
    public SessionListener sessionListener() {
        return new JTSessionListener();
    }

    @Bean
    public HandlerMapping handlerMapping() {
        return new SpringHandlerMapping();
    }

    @Bean
    public JTHandlerInterceptor handlerInterceptor() {
        return new JTHandlerInterceptor();
    }

    @Override
    public void afterPropertiesSet() {
        jt808Server.start();
    }

    @Override
    public void destroy() {
        jt808Server.stop();
    }
}