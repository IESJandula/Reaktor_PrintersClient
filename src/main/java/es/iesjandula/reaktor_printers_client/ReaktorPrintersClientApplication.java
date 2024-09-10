package es.iesjandula.reaktor_printers_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Francisco Manuel Benítez Chico
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"es.iesjandula"})
public class ReaktorPrintersClientApplication
{
	public static void main(String[] args)
	{
		SpringApplication.run(ReaktorPrintersClientApplication.class, args);
	}
}
