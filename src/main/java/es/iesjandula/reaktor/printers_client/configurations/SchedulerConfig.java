package es.iesjandula.reaktor.printers_client.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig
{
	@Value("${reaktor.threadsNumber}")
	private Integer threadsNumber ;
	
    @Bean
    public ThreadPoolTaskScheduler taskScheduler()
    {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler() ;
        
        taskScheduler.setPoolSize(this.threadsNumber) ;
        taskScheduler.setThreadNamePrefix("ScheduledTask-") ;
        
        return taskScheduler ;
    }
}
