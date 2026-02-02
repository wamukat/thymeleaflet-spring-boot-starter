package io.github.wamukat.thymeleaflet.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * テスト専用Bean設定
 * 
 * プロダクションコードがテスト環境を意識しないようにするため、
 * テスト用のBeanをここで定義する。
 * 
 * Clean Architecture原則:
 * - 関心の分離: テスト設定とプロダクション設定を分離
 * - 依存関係逆転: プロダクションコードがテスト詳細を知らない
 * 
 * アプローチ: 
 * 全てのthymeleafletBeanをテスト時に有効化
 */
@TestConfiguration
@ComponentScan(basePackages = "io.github.wamukat.thymeleaflet")
public class FragmentTestConfiguration {
    // ComponentScanによりthymeleafletの全Beanがテスト時に自動登録される
}