package top.mangod.springwebsocket.conf;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@Slf4j
@RefreshScope
public class WebConfiguration implements WebMvcConfigurer {


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

    }

    @Bean
    public CustomFilter identityFilter() {
        return new CustomFilter();
    }



    /**
     * 添加拦截器
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /*registry.addInterceptor(apiVerifyInterceptor)
                .addPathPatterns("/open/**")
                .excludePathPatterns("/swagger-resources/**", "/webjars/**");
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/open/**")
                .excludePathPatterns(noAuthUrlProperty.getNoAuthUrls());

        registry.addInterceptor(serviceContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/swagger-resources/**", "/webjars/**");*/
    }
}
