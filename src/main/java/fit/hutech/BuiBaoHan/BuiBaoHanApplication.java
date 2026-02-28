package fit.hutech.BuiBaoHan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BuiBaoHanApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuiBaoHanApplication.class, args);
	}

}
