package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import java.util.Locale;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.Order;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.DelegatingMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Storybook機能の自動設定クラス
 * 
 * Thymeleafフラグメントのストーリーブック機能を提供します。
 * thymeleaflet.enabledプロパティで機能の有効/無効を制御可能です。
 */
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ComponentScan(basePackages = "io.github.wamukat.thymeleaflet")
@EnableConfigurationProperties(StorybookProperties.class)
public class StorybookAutoConfiguration {
    
    /**
     * Storybook機能が有効化されたことをログ出力
     */
    public StorybookAutoConfiguration() {
        // 起動時のログ出力用
    }

    /**
     * Replace the default AcceptHeaderLocaleResolver with CookieLocaleResolver.
     */
    @Bean
    public static BeanDefinitionRegistryPostProcessor thymeleafletLocaleResolverPostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                if (!registry.containsBeanDefinition("localeResolver")) {
                    RootBeanDefinition resolver = new RootBeanDefinition(CookieLocaleResolver.class);
                    resolver.getPropertyValues().add("defaultLocale", Locale.ENGLISH);
                    resolver.getPropertyValues().add("cookieName", "thymeleaflet.lang");
                    resolver.getPropertyValues().add("cookieMaxAge", 60 * 60 * 24 * 365);
                    resolver.getPropertyValues().add("cookiePath", "/");
                    registry.registerBeanDefinition("localeResolver", resolver);
                    return;
                }

                BeanDefinition existing = registry.getBeanDefinition("localeResolver");
                String beanClassName = existing.getBeanClassName();
                String factoryBeanName = existing.getFactoryBeanName();

                boolean isDefaultResolver = (beanClassName != null && beanClassName.contains("AcceptHeaderLocaleResolver"))
                        || (factoryBeanName != null && factoryBeanName.contains("WebMvcAutoConfiguration"));

                if (!isDefaultResolver) {
                    return;
                }

                registry.removeBeanDefinition("localeResolver");
                RootBeanDefinition resolver = new RootBeanDefinition(CookieLocaleResolver.class);
                resolver.getPropertyValues().add("defaultLocale", Locale.ENGLISH);
                resolver.getPropertyValues().add("cookieName", "thymeleaflet.lang");
                resolver.getPropertyValues().add("cookieMaxAge", 60 * 60 * 24 * 365);
                resolver.getPropertyValues().add("cookiePath", "/");
                registry.registerBeanDefinition("localeResolver", resolver);
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            }
        };
    }

    /**
     * Thymeleaflet Starterの静的リソースハンドリングを設定
     * 最高優先度で処理されるよう調整
     */
    @Bean
    @Order(0) // 最高優先度
    public WebMvcConfigurer thymeleafletResourceConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // 静的リソース専用パスを限定（コントローラーマッピングと競合を避ける）
                registry.addResourceHandler("/thymeleaflet/css/**")
                        .addResourceLocations("classpath:/META-INF/resources/static/css/");
                registry.addResourceHandler("/thymeleaflet/js/**")
                        .addResourceLocations("classpath:/META-INF/resources/static/js/");
                registry.addResourceHandler("/thymeleaflet/images/**")
                        .addResourceLocations("classpath:/META-INF/resources/static/images/");
                        
                // CSS専用ハンドラー（fallback）（キャッシュなし）
                registry.addResourceHandler("/css/thymeleaflet.css")
                        .addResourceLocations("classpath:/META-INF/resources/static/css/");
            }
        };
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(LocaleChangeInterceptor.class)
    public LocaleChangeInterceptor thymeleafletLocaleChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Bean
    public WebMvcConfigurer thymeleafletLocaleConfigurer(LocaleChangeInterceptor thymeleafletLocaleChangeInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(thymeleafletLocaleChangeInterceptor);
                registry.addInterceptor(new ThymeleafletLocaleContextInterceptor());
            }
        };
    }

    @Bean
    public BeanPostProcessor thymeleafletMessageSourcePostProcessor() {
        return new BeanPostProcessor() {
            private final MessageSource thymeleafletMessageSource = createThymeleafletMessageSource();

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (!"messageSource".equals(beanName)) {
                    return bean;
                }

                if (bean instanceof AbstractMessageSource abstractMessageSource) {
                    if (abstractMessageSource.getParentMessageSource() == null) {
                        abstractMessageSource.setParentMessageSource(thymeleafletMessageSource);
                    }
                } else if (bean instanceof DelegatingMessageSource delegatingMessageSource) {
                    if (delegatingMessageSource.getParentMessageSource() == null) {
                        delegatingMessageSource.setParentMessageSource(thymeleafletMessageSource);
                    }
                }

                return bean;
            }
        };
    }

    private static MessageSource createThymeleafletMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
