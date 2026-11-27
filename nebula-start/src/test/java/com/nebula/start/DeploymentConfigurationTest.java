package com.nebula.start;

import com.nebula.start.config.DatabaseCredentialValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentConfigurationTest {
    @TempDir Path directory;

    private ApplicationContextRunner application() throws Exception {
        Files.writeString(directory.resolve("NEBULA_DB_USERNAME"), "configuration_fixture");
        Files.writeString(directory.resolve("NEBULA_DB_PASSWORD"), "fixture-" + UUID.randomUUID());
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withPropertyValues("spring.config.location=classpath:application.yaml",
                        "spring.config.import=optional:configtree:" + directory.toAbsolutePath().toString().replace('\\', '/') + "/",
                        "MAIL_USERNAME=configuration@example.invalid", "MAIL_PASSWORD=mail-configuration-fixture")
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withBean(DatabaseCredentialValidation.class);
    }

    private ApplicationContextRunner environment(ApplicationContextRunner runner, Map<String, Object> values) {
        return runner.withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("isolated-deployment-environment", values)));
    }

    @Test
    void actualApplicationConfigurationLoadsExternalDatabaseCredentials() throws Exception {
        application().run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals("configuration_fixture", ctx.getEnvironment().getProperty("spring.datasource.username"));
            assertEquals(Files.readString(directory.resolve("NEBULA_DB_PASSWORD")),
                    ctx.getEnvironment().getProperty("spring.datasource.password"));
        });
    }

    @Test
    void environmentCredentialsOverrideTheLocalFiles() throws Exception {
        String password = "external-" + UUID.randomUUID();
        environment(application(), Map.of("NEBULA_DB_USERNAME", "deployment_fixture", "NEBULA_DB_PASSWORD", password))
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    assertEquals("deployment_fixture", ctx.getEnvironment().getProperty("spring.datasource.username"));
                    assertEquals(password, ctx.getEnvironment().getProperty("spring.datasource.password"));
                });
    }

    @Test
    void emptyEnvironmentPasswordCannotFallBackToTheLocalSecret() throws Exception {
        environment(application(), Map.of("NEBULA_DB_PASSWORD", ""))
                .run(ctx -> assertFailure(ctx.getStartupFailure(), "NEBULA_DB_PASSWORD"));
    }

    @Test
    void blankApplicationAccountPreventsStartup() throws Exception {
        environment(application(), Map.of("NEBULA_DB_USERNAME", "  "))
                .run(ctx -> assertFailure(ctx.getStartupFailure(), "NEBULA_DB_USERNAME"));
    }

    @Test
    void aRootOverrideIsRejectedWithoutRevealingThePassword() throws Exception {
        String password = "private-value-" + UUID.randomUUID();
        environment(application(), Map.of("NEBULA_DB_USERNAME", "ROOT", "NEBULA_DB_PASSWORD", password))
                .run(ctx -> {
                    assertFailure(ctx.getStartupFailure(), "non-root");
                    var trace = new StringWriter();
                    ctx.getStartupFailure().printStackTrace(new PrintWriter(trace));
                    assertFalse(trace.toString().contains(password));
                });
    }

    @Test
    void anUnresolvedPasswordPlaceholderPreventsStartup() throws Exception {
        application().withPropertyValues("spring.datasource.password=${NEBULA_CONFIGURATION_TEST_ABSENT_PASSWORD}")
                .run(ctx -> assertFailure(ctx.getStartupFailure(), "NEBULA_CONFIGURATION_TEST_ABSENT_PASSWORD"));
    }

    @Test
    void theActualMailSessionDisablesProtocolDebugWithoutSendingMail() throws Exception {
        application().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class)).run(ctx -> {
            assertNull(ctx.getStartupFailure());
            JavaMailSenderImpl sender = ctx.getBean(JavaMailSenderImpl.class);
            assertEquals("configuration@example.invalid", sender.getUsername());
            assertEquals("mail-configuration-fixture", sender.getPassword());
            assertEquals("false", sender.getJavaMailProperties().getProperty("mail.debug"));
            assertFalse(sender.getSession().getDebug());
            assertNotNull(sender.createMimeMessage());
        });
    }

    @Test
    void httpJsonKeepsTheExistingDateFormatWithBothJacksonVersionsPresent() {
        new WebApplicationContextRunner()
                .withPropertyValues("spring.config.location=classpath:application.yaml")
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, Jackson2AutoConfiguration.class,
                        HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class))
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    var selected = ctx.getBean(RequestMappingHandlerAdapter.class).getMessageConverters().stream()
                            .filter(converter -> converter.canWrite(Date.class, MediaType.APPLICATION_JSON))
                            .findFirst().orElseThrow();
                    var converter = assertInstanceOf(MappingJackson2HttpMessageConverter.class, selected);
                    var date = Date.from(Instant.parse("2026-11-26T00:00:00Z"));
                    assertEquals("\"2026-11-26 08:00:00\"", converter.getObjectMapper().writeValueAsString(date));
                });
    }

    private static void assertFailure(Throwable failure, String safeMessage) {
        assertNotNull(failure);
        while (failure.getCause() != null) failure = failure.getCause();
        assertTrue(failure.getMessage().contains(safeMessage));
    }
}
