package ru.vtb.selftest.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Проверка инфраструктуры selftest: подстановка реестра и пропуск тестов без Docker.
 */
public class DockerSupportTest {

    private static final String IMAGE = "alpine:3.20";

    @Test(groups = {"selftest"}, description = "Без selftest.registry образ берётся с Docker Hub без изменений, "
            + "с ним — с указанным префиксом (Docker не нужен)")
    public void imageNameRespectsRegistryProperty() {
        String prefix = DockerSupport.registryPrefix();
        String actual = ImageNameSubstitutor.instance().apply(DockerImageName.parse(IMAGE)).asCanonicalNameString();
        String expected = prefix.isEmpty() ? IMAGE : (prefix.endsWith("/") ? prefix : prefix + "/") + IMAGE;
        assertEquals(actual, expected, "Имя образа при selftest.registry='%s'".formatted(prefix));
    }

    @Test(groups = {"selftest"}, description = "Подстановщик: префикс только для образов Docker Hub, слеш добавляется сам")
    public void substitutorAddsPrefixOnlyToDockerHubImages() {
        String saved = System.getProperty(DockerSupport.REGISTRY_PROPERTY);
        RegistryImageNameSubstitutor substitutor = new RegistryImageNameSubstitutor();
        try {
            System.setProperty(DockerSupport.REGISTRY_PROPERTY, "registry.example.com/mirror");
            assertEquals(substitutor.apply(DockerImageName.parse("alpine:3.20")).asCanonicalNameString(),
                    "registry.example.com/mirror/alpine:3.20");
            assertEquals(substitutor.apply(DockerImageName.parse("testcontainers/ryuk:0.12.0")).asCanonicalNameString(),
                    "registry.example.com/mirror/testcontainers/ryuk:0.12.0");
            assertEquals(substitutor.apply(DockerImageName.parse("quay.io/org/image:1")).asCanonicalNameString(),
                    "quay.io/org/image:1", "Образ с явным реестром не меняется");
            System.setProperty(DockerSupport.REGISTRY_PROPERTY, "");
            assertEquals(substitutor.apply(DockerImageName.parse("alpine:3.20")).asCanonicalNameString(), "alpine:3.20");
        } finally {
            if (saved == null) {
                System.clearProperty(DockerSupport.REGISTRY_PROPERTY);
            } else {
                System.setProperty(DockerSupport.REGISTRY_PROPERTY, saved);
            }
        }
    }

    @Test(groups = {"selftest"}, description = "Контейнер запускается, если Docker доступен; иначе тест пропускается")
    public void containerStartsWhenDockerAvailable() {
        DockerSupport.requireDocker();
        try (GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withCommand("sleep", "30")) {
            container.start();
            assertTrue(container.isRunning(), "Контейнер %s должен быть запущен".formatted(IMAGE));
        }
    }
}
