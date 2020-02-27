package com.example.FTP;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Locale;

import ch.qos.logback.classic.Logger;
import org.apache.commons.net.ftp.FTPSClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.ftp.outbound.FtpMessageHandler;
import org.springframework.integration.ftp.session.DefaultFtpsSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;

@SpringBootApplication
public class FtpApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(FtpApplication.class)
                        .run(args);
        MyGateway gateway = context.getBean(MyGateway.class);
        gateway.sendToFtp(new File("C:\\Users\\lenovo\\Pictures\\ferreteria\\cocina.jpg"));
    }

    @Bean
    public DefaultFtpsSessionFactory sf() {
        DefaultFtpsSessionFactory sf = new DefaultFtpsSessionFactory() {

            @Override
            protected FTPSClient createClientInstance() {
                return new SharedSSLFTPSClient();
            }

        };
        sf.setHost("192.168.200.20");
        sf.setPort(22);
        sf.setUsername("desarollo");
        sf.setPassword("Int3redes1");
        sf.setNeedClientAuth(true);
        return sf;
    }

    private static final class SharedSSLFTPSClient extends FTPSClient {

        @Override
        protected void _prepareDataSocket_(final Socket socket) throws IOException {
            if (socket instanceof SSLSocket) {
                // Control socket is SSL
                final SSLSession session = ((SSLSocket) _socket_).getSession();
                final SSLSessionContext context = session.getSessionContext();
                context.setSessionCacheSize(0); // you might want to limit the cache
                Logger logger= null;
                try {
                    final Field sessionHostPortCache = context.getClass()
                            .getDeclaredField("sessionHostPortCache");
                    sessionHostPortCache.setAccessible(true);
                    final Object cache = sessionHostPortCache.get(context);
                    final Method method = cache.getClass().getDeclaredMethod("put", Object.class,
                            Object.class);
                    method.setAccessible(true);
                    String key = String.format("%s:%s", socket.getInetAddress().getHostName(),
                            String.valueOf(socket.getPort())).toLowerCase(Locale.ROOT);
                    method.invoke(cache, key, session);
                    key = String.format("%s:%s", socket.getInetAddress().getHostAddress(),
                            String.valueOf(socket.getPort())).toLowerCase(Locale.ROOT);
                    method.invoke(cache, key, session);
                }
                catch (NoSuchFieldException e) {
                    // Not running in expected JRE
                    logger.warn("No field sessionHostPortCache in SSLSessionContext", e);
                }
                catch (Exception e) {
                    // Not running in expected JRE
                    logger.warn(e.getMessage());
                }
            }

        }

    }

    @Bean
    @ServiceActivator(inputChannel = "toFtpChannel")
    public MessageHandler handler() {
        FtpMessageHandler handler = new FtpMessageHandler(sf());
        handler.setRemoteDirectoryExpressionString("headers['/home/desarollo/palermodev/fotos_francisco']");
        handler.setFileNameGenerator(new FileNameGenerator() {

            @Override
            public String generateFileName(Message<?> message) {
                return "handlerContent.test";
            }

        });
        return handler;
    }

    @MessagingGateway
    public interface MyGateway {

        @Gateway(requestChannel = "toFtpChannel")
        void sendToFtp(File file);

    }
}