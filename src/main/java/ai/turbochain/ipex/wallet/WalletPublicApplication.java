package ai.turbochain.ipex.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class WalletPublicApplication {
    public static void main(String[] args){
        SpringApplication.run(WalletPublicApplication.class,args);
    }
}
