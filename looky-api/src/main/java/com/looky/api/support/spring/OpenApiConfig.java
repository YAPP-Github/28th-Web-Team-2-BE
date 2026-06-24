package com.looky.api.support.spring;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Looky API",
                version = "v1",
                description = "Looky 설문 생성, 응답, 결과 조회 API 문서"
        )
)
public class OpenApiConfig {
}
