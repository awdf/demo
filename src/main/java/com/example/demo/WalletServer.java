package com.example.demo;

import com.example.demo.boot.BootListener;
import com.example.demo.boot.ContextRefreshListener;
import com.example.demo.services.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableJpaAuditing
public class WalletServer extends SpringBootServletInitializer {
    private static final Logger log = LoggerFactory.getLogger(WalletServer.class);

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(WalletServer.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(WalletServer.class, args);
    }

    @PostConstruct
    private void doInit(){
        log.info("Application constructor");
    }

    @Bean
    public ContextRefreshListener getContextRefreshedListener(){
        return new ContextRefreshListener();
    }

    @Bean
    public CommandLineRunner getCommandLineRunner(ApplicationContext ctx) {
        return (args) -> {
            ctx.getBeansOfType(BaseService.class).forEach((k,v)->{
                log.info("Found service implementation: " + k);
            });
            log.info("");
        };
    }

    @Bean
    public BootListener getBootListener(){
        return new BootListener();
    }

}