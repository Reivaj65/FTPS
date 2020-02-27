package com.example.FTP;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.*;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.outbound.FtpMessageHandler;
import org.springframework.integration.ftp.session.DefaultFtpsSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Locale;

@Configuration
class FTPConfig {


    @Bean
    public SessionFactory<FTPFile> ftpSessionFactory() {
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
        return new CachingSessionFactory<FTPFile>(sf);
    }

    private static final class SharedSSLFTPSClient extends FTPSClient {

        @Override
        protected void _prepareDataSocket_(final Socket socket) throws IOException {
            if (socket instanceof SSLSocket) {
                // Control socket is SSL
                final SSLSession session = ((SSLSocket) _socket_).getSession();
                final SSLSessionContext context = session.getSessionContext();
                context.setSessionCacheSize(0); // you might want to limit the cache
                Logger logger = null;
                try {
                    final Field sessionHostPortCache = context.getClass()
                        .getDeclaredField("sessionHostPortCache");
                    sessionHostPortCache.setAccessible(true);
                    final Object cache = sessionHostPortCache.get(context);
                    final Method method = cache.getClass().getDeclaredMethod("put", Object.class,
                        Object.class);
                    method.setAccessible(true);
                    String key = String.format("%s:%s", socket.getInetAddress().getHostName(),
                        socket.getPort()).toLowerCase(Locale.ROOT);
                    method.invoke(cache, key, session);
                    key = String.format("%s:%s", socket.getInetAddress().getHostAddress(),
                        (socket.getPort())).toLowerCase(Locale.ROOT);
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
    @ServiceActivator(inputChannel = "ftpChannel")
    public MessageHandler handler() {
        FtpMessageHandler handler = new FtpMessageHandler(ftpSessionFactory());
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
    interface MyGateway {

        @Gateway(requestChannel = "ftpChannel")
        void sendToFtp(File file);

    }
    
}