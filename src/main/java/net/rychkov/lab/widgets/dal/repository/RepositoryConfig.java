package net.rychkov.lab.widgets.dal.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    private final ApplicationContext appContext;

    @Value("${widgets.repository.mode}")
    private String mode;

    public RepositoryConfig(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @Bean("repository")
    public WidgetRepository widgetRepository() {
        if ("h2".equals(mode)) {
            return (WidgetRepository) appContext.getBean("h2");
        }
        return (WidgetRepository) appContext.getBean("customInMemory");
    }

}
