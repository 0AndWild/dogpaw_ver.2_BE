package com.project.dogfaw.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerConfig {

    /*Docket은 Swagger 설정의 핵심이 되는 Bean*/
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .useDefaultResponseMessages(false) //Swagger에서 제공해주는 기본 응답코드. false로 설정하면 기본응답x
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.project.dogfaw.user.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Dogpaw Swagger")
                .description("dogpaw swagger config")
                .version("1.0")
                .build();
    }
}
