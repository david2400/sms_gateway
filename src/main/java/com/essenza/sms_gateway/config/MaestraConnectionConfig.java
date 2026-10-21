//package com.essenza.sms_gateway.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.jdbc.DataSourceBuilder;
//import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//import jakarta.persistence.EntityManagerFactory;
//import javax.sql.DataSource;
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//@EnableTransactionManagement
//@EnableJpaRepositories(
//    basePackages = {
//        "com.essenza.sms_gateway.modules.malla_notificacion.infrastructure.outbound.repositories",
//        "com.essenza.sms_gateway.modules.notificar.CreateNotificacion.infrastructure.outbound.repositories",
//        "com.essenza.sms_gateway.modules.notificar.PublishNotificacion.infrastructure.outbound.persistence",
//        "com.essenza.sms_gateway.modules.EmailService.infrastructure.outbound.repositories",
//        "com.essenza.sms_gateway.modules.plantillas.infrastructure.outbound.persistence",
//        "com.essenza.sms_gateway.modules.plantillas.infrastructure.outbound.repository",
//        "com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository",
//        "com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository"
//    },
//    entityManagerFactoryRef = "maestraEntityManagerFactory",
//    transactionManagerRef = "maestraTransactionManager"
//)
//public class MaestraConnectionConfig {
//
//    @Autowired
//    private MaestraPackageScanner packageScanner;
//
//    @Bean(name = "maestraDataSource")
//    @ConfigurationProperties(prefix = "spring.datasource")
//    public DataSource maestraDataSource() {
//        return DataSourceBuilder.create()
//                .build();
//    }
//
//    @Bean(name = "maestraEntityManagerFactory")
//    public LocalContainerEntityManagerFactoryBean
//    entityManagerFactory(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("maestraDataSource") DataSource dataSource
//    ) {
//        return builder
//                .dataSource(dataSource)
//                .packages(
//                        "com.essenza.sms_gateway.modules.malla_notificacion.infrastructure.outbound.persistence.sqlserver.maestra",
//                        "com.essenza.sms_gateway.modules.notificar.CreateNotificacion.infrastructure.outbound.persistence.sqlserver.maestra",
//                        "com.essenza.sms_gateway.modules.notificar.PublishNotificacion.infrastructure.outbound.persistence",
//                        "com.essenza.sms_gateway.modules.EmailService.infrastructure.outbound.persistence",
//                        "com.essenza.sms_gateway.modules.plantillas.infrastructure.outbound.persistence.sqlserver.maestra",
//                        "com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity",
//                        "com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity"
//                )
//                .persistenceUnit("maestra")
//                .properties(jpaProperties())
//                .build();
//    }
//
//    @Bean(name = "maestraTransactionManager")
//    public PlatformTransactionManager maestraTransactionManager(
//            @Qualifier("maestraEntityManagerFactory") EntityManagerFactory customerEntityManagerFactory
//    ) {
//        return new JpaTransactionManager(customerEntityManagerFactory);
//    }
//
//    private Map<String, Object> jpaProperties() {
//        Map<String, Object> properties = new HashMap<>();
//        // El dialecto se autodetecta (SQL Server: SQLServerDialect, MySQL: MySQLDialect)
//        properties.put("hibernate.hbm2ddl.auto", "update");
//        properties.put("hibernate.show_sql", "true");
//        properties.put("hibernate.format_sql", "true");
//        properties.put("hibernate.jdbc.time_zone", "UTC");
//        properties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
//        return properties;
//    }
//}
