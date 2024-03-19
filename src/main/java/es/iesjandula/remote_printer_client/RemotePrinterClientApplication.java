package es.iesjandula.remote_printer_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "es.iesjandula.remote_printer_client")
public class RemotePrinterClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemotePrinterClientApplication.class, args);
	}

}
