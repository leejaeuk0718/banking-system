package com.jaeuk.job_ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 설정은 SecurityConfig#corsConfigurationSource() 한 곳으로 일원화한다.
 * (Spring Security 가 활성화된 경우 SecurityConfig 쪽 설정이 우선되며 두 곳에 두면
 *  설정이 어긋날 때 디버깅이 어려워진다.)
 *
 * 이 클래스는 향후 인터셉터/리졸버/포매터 등 WebMvc 커스터마이징을 추가할 자리만 남겨둔다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
