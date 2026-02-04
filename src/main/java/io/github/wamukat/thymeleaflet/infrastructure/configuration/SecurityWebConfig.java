package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

/**
 * セキュリティ関連Web設定
 * 
 * インターセプター登録、非同期実行設定
 */
@Configuration
@EnableAsync
@EnableScheduling
public class SecurityWebConfig implements WebMvcConfigurer {
    
    // SecurityInterceptorは開発者用ライブラリには不要のため削除済み
    
    /**
     * セキュリティ処理用非同期タスクエグゼキューター
     */
    @Bean("securityTaskExecutor")
    public Executor securityTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // スレッドプール設定
        executor.setCorePoolSize(2);      // 基本スレッド数
        executor.setMaxPoolSize(10);      // 最大スレッド数
        executor.setQueueCapacity(100);   // キュー容量
        executor.setKeepAliveSeconds(60); // アイドル時間
        
        // スレッド名設定
        executor.setThreadNamePrefix("security-");
        
        // 拒否ポリシー: CallerRunsPolicy（呼び出し元スレッドで実行）
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // スレッドの適切な終了
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * 通知処理用非同期タスクエグゼキューター
     */
    @Bean("notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(1);      // 通知は軽量なので少なめ
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        
        executor.initialize();
        return executor;
    }
}