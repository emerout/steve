package de.rwth.idsg.steve.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.rwth.idsg.steve.service.DummyReleaseCheckService;
import de.rwth.idsg.steve.service.GithubReleaseCheckService;
import de.rwth.idsg.steve.service.ReleaseCheckService;
import de.rwth.idsg.steve.utils.InternetChecker;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.annotation.PreDestroy;
import javax.validation.Validator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static de.rwth.idsg.steve.SteveConfiguration.CONFIG;

/**
 * Configuration and beans of Spring Framework.
 *
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 15.08.2014
 */
@Slf4j
@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan("de.rwth.idsg.steve")
public class BeanConfiguration extends WebMvcConfigurerAdapter {

    private HikariDataSource dataSource;
    private ScheduledThreadPoolExecutor executor;

    /**
     * https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
     */
    private void initDataSource() {

        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName(MysqlDataSource.class.getName());

        config.addDataSourceProperty("serverName", CONFIG.getDb().getIp());
        config.addDataSourceProperty("port", CONFIG.getDb().getPort());
        config.addDataSourceProperty("databaseName", CONFIG.getDb().getSchema());
        config.addDataSourceProperty("user", CONFIG.getDb().getUserName());
        config.addDataSourceProperty("password", CONFIG.getDb().getPassword());

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // we must explicitly set the encoding, otherwise data is not handled properly!
        config.addDataSourceProperty("encoding", "utf8");
        config.addDataSourceProperty("useUnicode", "true");

        dataSource = new HikariDataSource(config);
    }

    @Bean
    @Qualifier("jooqConfig")
    public org.jooq.Configuration jooqConfig() {
        initDataSource();

        Settings settings = new Settings()
                // Normally, the records are "attached" to the Configuration that created (i.e. fetch/insert) them.
                // This means that they hold an internal reference to the same database connection that was used.
                // The idea behind this is to make CRUD easier for potential subsequent store/refresh/delete
                // operations. We do not use or need that.
                .withAttachRecords(false)
                // To log or not to log the sql queries, that is the question
                .withExecuteLogging(CONFIG.getDb().isSqlLogging());

        // Configuration for JOOQ
        return new DefaultConfiguration()
                .set(SQLDialect.MYSQL)
                .set(new DataSourceConnectionProvider(dataSource))
                .set(settings);
    }

    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Can we re-use DSLContext as a Spring bean (singleton)? Yes, the Spring tutorial of
     * Jooq also does it that way, but only if we do not change anything about the
     * config after the init (which we don't do anyways) and if the ConnectionProvider
     * does not store any shared state (we use DataSourceConnectionProvider of Jooq, so no problem).
     *
     * Some sources and discussion:
     * - http://www.jooq.org/doc/3.6/manual/getting-started/tutorials/jooq-with-spring/
     * - http://jooq-user.narkive.com/2fvuLodn/dslcontext-and-threads
     * - https://groups.google.com/forum/#!topic/jooq-user/VK7KQcjj3Co
     * - http://stackoverflow.com/questions/32848865/jooq-dslcontext-correct-autowiring-with-spring
     */
    @Bean
    public DSLContext dslContext() {
        return DSL.using(jooqConfig());
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("SteVe-Executor-%d")
                                                                .build();

        executor = new ScheduledThreadPoolExecutor(5, threadFactory);
        return executor;
    }

    /**
     * There might be instances deployed in a local/closed network with no internet connection. In such situations,
     * it is unnecessary to try to access Github every time, even though the request will time out and result
     * report will be correct (that there is no new version). With DummyReleaseCheckService we bypass the intermediate
     * steps and return a "no new version" report immediately.
     */
    @Bean
    public ReleaseCheckService releaseCheckService() {
        if (InternetChecker.isInternetAvailable()) {
            return new GithubReleaseCheckService();
        } else {
            return new DummyReleaseCheckService();
        }
    }

    @PreDestroy
    public void shutDown() {
        if (dataSource != null) {
            dataSource.close();
        }

        if (executor != null) {
            gracefulShutDown(executor);
        }
    }

    private void gracefulShutDown(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            log.error("Termination interrupted", e);

        } finally {
            if (!executor.isTerminated()) {
                log.warn("Killing non-finished tasks");
            }
            executor.shutdownNow();
        }
    }

    // -------------------------------------------------------------------------
    // Web config
    // -------------------------------------------------------------------------

    /**
     * Resolver for JSP views/templates. Controller classes process the requests
     * and forward to JSP files for rendering.
     */
    @Bean
    public InternalResourceViewResolver urlBasedViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    /**
     * Resource path for static content of the Web interface.
     */
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/manager/signin").setViewName("signin");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

}
