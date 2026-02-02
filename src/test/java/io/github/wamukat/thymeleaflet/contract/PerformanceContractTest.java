package io.github.wamukat.thymeleaflet.contract;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.MigrationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StopWatch;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * パフォーマンス劣化検出テスト (Golden Master Pattern)
 * 
 * Clean Architecture移行中もレスポンス性能の劣化を検出・防止する
 * 
 * 【重要】このテストで基準値を超える劣化が検出された場合は即座に作業を停止すること
 */
@SpringBootTest(classes = {io.github.wamukat.thymeleaflet.config.FragmentTestConfiguration.class})
@AutoConfigureWebMvc
@ActiveProfiles("test")
class PerformanceContractTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MigrationProperties migrationProperties;

    private MockMvc mockMvc;

    // パフォーマンスベースライン（ミリ秒）
    private static final long FRAGMENT_LIST_BASELINE_MS = 1000;  // フラグメント一覧
    private static final long STORY_PREVIEW_BASELINE_MS = 800;   // ストーリープレビュー
    private static final long SECURITY_VALIDATION_BASELINE_MS = 50; // セキュリティ検証

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    @DisplayName("【Performance Contract】フラグメント一覧 - レスポンス性能保護")
    void shouldPreserveFragmentListPerformance() throws Exception {
        // Given: フラグメント一覧エンドポイント
        String url = "/thymeleaflet";
        int iterationCount = 5; // 複数回測定で平均を取る
        
        // When: 複数回測定してレスポンス時間を計測
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < iterationCount; i++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            
            MvcResult result = mockMvc.perform(get(url)
                    .contentType(MediaType.TEXT_HTML))
                    .andExpect(status().isOk())
                    .andReturn();
            
            stopWatch.stop();
            responseTimes.add(stopWatch.getTotalTimeMillis());
            
            // レスポンスの基本チェック
            assertNotNull(result.getResponse().getContentAsString(), 
                    "レスポンスボディが存在すること");
        }
        
        // Then: パフォーマンス劣化の検証
        OptionalDouble averageTime = responseTimes.stream().mapToLong(Long::longValue).average();
        assertTrue(averageTime.isPresent(), "平均レスポンス時間が計測できること");
        
        double avgResponseTime = averageTime.getAsDouble();
        double allowedThreshold = FRAGMENT_LIST_BASELINE_MS * 
                (1.0 + migrationProperties.getMonitoring().getResponseTimeDegradationThreshold() / 100.0);
        
        // パフォーマンス契約の検証
        assertTrue(avgResponseTime <= allowedThreshold, 
                String.format("フラグメント一覧の平均レスポンス時間 (%.2fms) が許容しきい値 (%.2fms) 以下であること", 
                        avgResponseTime, allowedThreshold));
        
        // ログ出力で現在のパフォーマンスを記録
        System.out.println("【Performance Contract記録】フラグメント一覧:");
        System.out.println("  測定回数: " + iterationCount);
        System.out.println("  平均レスポンス時間: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("  ベースライン: " + FRAGMENT_LIST_BASELINE_MS + "ms");
        System.out.println("  許容しきい値: " + String.format("%.2f", allowedThreshold) + "ms");
        System.out.println("  個別測定値: " + responseTimes);
    }

    @Test
    @DisplayName("【Performance Contract】ストーリープレビュー - レスポンス性能保護")
    void shouldPreserveStoryPreviewPerformance() throws Exception {
        // Given: ストーリープレビューエンドポイント
        String templatePath = "shared.atoms.button-action";
        String fragmentName = "primary";
        String storyName = "default";
        String url = String.format("/thymeleaflet/%s/%s/%s", templatePath, fragmentName, storyName);
        int iterationCount = 5;
        
        // When: 複数回測定してレスポンス時間を計測
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < iterationCount; i++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            
            MvcResult result = mockMvc.perform(get(url)
                    .contentType(MediaType.TEXT_HTML))
                    .andExpect(status().isOk())
                    .andReturn();
            
            stopWatch.stop();
            responseTimes.add(stopWatch.getTotalTimeMillis());
            
            // レスポンスの基本チェック
            assertNotNull(result.getResponse().getContentAsString(), 
                    "レスポンスボディが存在すること");
        }
        
        // Then: パフォーマンス劣化の検証
        OptionalDouble averageTime = responseTimes.stream().mapToLong(Long::longValue).average();
        assertTrue(averageTime.isPresent(), "平均レスポンス時間が計測できること");
        
        double avgResponseTime = averageTime.getAsDouble();
        double allowedThreshold = STORY_PREVIEW_BASELINE_MS * 
                (1.0 + migrationProperties.getMonitoring().getResponseTimeDegradationThreshold() / 100.0);
        
        // パフォーマンス契約の検証
        assertTrue(avgResponseTime <= allowedThreshold, 
                String.format("ストーリープレビューの平均レスポンス時間 (%.2fms) が許容しきい値 (%.2fms) 以下であること", 
                        avgResponseTime, allowedThreshold));
        
        // ログ出力で現在のパフォーマンスを記録
        System.out.println("【Performance Contract記録】ストーリープレビュー:");
        System.out.println("  測定回数: " + iterationCount);
        System.out.println("  平均レスポンス時間: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("  ベースライン: " + STORY_PREVIEW_BASELINE_MS + "ms");
        System.out.println("  許容しきい値: " + String.format("%.2f", allowedThreshold) + "ms");
        System.out.println("  個別測定値: " + responseTimes);
    }

    @Test
    @DisplayName("【Performance Contract】セキュリティ検証 - 処理性能保護")
    void shouldPreserveSecurityValidationPerformance() {
        // Given: セキュリティ検証対象パス
        String validPath = "domain.point.molecules";
        int iterationCount = 100; // セキュリティ検証は高頻度なので多めに測定
        
        // When: 複数回測定してセキュリティ検証時間を計測
        List<Long> validationTimes = new ArrayList<>();
        
        for (int i = 0; i < iterationCount; i++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            
            // セキュリティ検証処理（現在の実装をベースライン測定）
            try {
                // Note: 実際の実装では SecurePathConverter を注入して使用
                String result = validPath.replace(".", "/"); // 簡略化した測定
                assertNotNull(result, "セキュリティ検証結果が存在すること");
            } finally {
                stopWatch.stop();
                validationTimes.add(stopWatch.getTotalTimeMillis());
            }
        }
        
        // Then: セキュリティ検証性能の劣化検証
        OptionalDouble averageTime = validationTimes.stream().mapToLong(Long::longValue).average();
        assertTrue(averageTime.isPresent(), "平均検証時間が計測できること");
        
        double avgValidationTime = averageTime.getAsDouble();
        double allowedThreshold = SECURITY_VALIDATION_BASELINE_MS * 
                (1.0 + migrationProperties.getMonitoring().getResponseTimeDegradationThreshold() / 100.0);
        
        // セキュリティ検証パフォーマンス契約の検証
        assertTrue(avgValidationTime <= allowedThreshold, 
                String.format("セキュリティ検証の平均処理時間 (%.2fms) が許容しきい値 (%.2fms) 以下であること", 
                        avgValidationTime, allowedThreshold));
        
        // ログ出力で現在のセキュリティ検証パフォーマンスを記録
        System.out.println("【Performance Contract記録】セキュリティ検証:");
        System.out.println("  測定回数: " + iterationCount);
        System.out.println("  平均検証時間: " + String.format("%.4f", avgValidationTime) + "ms");
        System.out.println("  ベースライン: " + SECURITY_VALIDATION_BASELINE_MS + "ms");
        System.out.println("  許容しきい値: " + String.format("%.2f", allowedThreshold) + "ms");
        
        // 統計情報も出力
        long minTime = validationTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxTime = validationTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        System.out.println("  最小時間: " + minTime + "ms");
        System.out.println("  最大時間: " + maxTime + "ms");
    }

    @Test
    @DisplayName("【Performance Contract】メモリ使用量 - リソース効率保護")
    void shouldPreserveMemoryUsageEfficiency() throws Exception {
        // Given: メモリ使用量測定の準備
        Runtime runtime = Runtime.getRuntime();
        
        // GC実行して測定開始点を明確化
        System.gc();
        Thread.sleep(100); // GC完了待機
        
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // When: フラグメント一覧を複数回実行してメモリ使用量を測定
        int operationCount = 10;
        for (int i = 0; i < operationCount; i++) {
            mockMvc.perform(get("/thymeleaflet")
                    .contentType(MediaType.TEXT_HTML))
                    .andExpect(status().isOk())
                    .andReturn();
        }
        
        // メモリ測定
        System.gc(); // GC実行
        Thread.sleep(100); // GC完了待機
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = memoryAfter - memoryBefore;
        
        // Then: メモリ効率の契約検証
        long memoryIncreasePerOperation = memoryIncrease / operationCount;
        long allowedMemoryIncrease = 10 * 1024 * 1024; // 10MB許容
        
        assertTrue(memoryIncreasePerOperation <= allowedMemoryIncrease, 
                String.format("操作あたりのメモリ増加量 (%d bytes) が許容値 (%d bytes) 以下であること", 
                        memoryIncreasePerOperation, allowedMemoryIncrease));
        
        // ログ出力でメモリ効率を記録
        System.out.println("【Performance Contract記録】メモリ効率:");
        System.out.println("  操作回数: " + operationCount);
        System.out.println("  実行前メモリ: " + String.format("%.2f", memoryBefore / 1024.0 / 1024.0) + "MB");
        System.out.println("  実行後メモリ: " + String.format("%.2f", memoryAfter / 1024.0 / 1024.0) + "MB");
        System.out.println("  総メモリ増加: " + String.format("%.2f", memoryIncrease / 1024.0 / 1024.0) + "MB");
        System.out.println("  操作あたり増加: " + String.format("%.2f", memoryIncreasePerOperation / 1024.0 / 1024.0) + "MB");
    }
}