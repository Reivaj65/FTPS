package com.example.FTP;

import java.io.File;
import java.util.List;
 
import com.example.FTP.FTPConfig.MyGateway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
 
@SpringBootApplication
public class FtpApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(FtpApplication.class)
                        .run(args);
        MyGateway gateway = context.getBean(MyGateway.class);
        gateway.sendToFtp(new File("C:/Users/lenovo/Pictures/Imagenes pc/city.jpg"));
    }
}
