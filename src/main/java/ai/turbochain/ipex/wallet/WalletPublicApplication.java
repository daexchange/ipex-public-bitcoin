package ai.turbochain.ipex.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableEurekaClient
@SpringBootApplication
@EnableScheduling
public class WalletPublicApplication {
    public static void main(String[] args){
        SpringApplication.run(WalletPublicApplication.class,args);
    }
}
