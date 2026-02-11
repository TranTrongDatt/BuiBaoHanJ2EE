package fit.hutech.BuiBaoHan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Scheduling configuration for cron jobs and scheduled tasks
 */
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        
        // Pool size for scheduled tasks
        threadPoolTaskScheduler.setPoolSize(5);
        
        // Thread name prefix
        threadPoolTaskScheduler.setThreadNamePrefix("MiniVerse-Scheduled-");
        
        // Wait for tasks to complete on shutdown
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        
        // Await termination seconds
        threadPoolTaskScheduler.setAwaitTerminationSeconds(60);
        
        // Error handler
        threadPoolTaskScheduler.setErrorHandler(t -> 
                System.err.println("Scheduled task error: " + t.getMessage()));
        
        threadPoolTaskScheduler.initialize();
        
        taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
    }
}
