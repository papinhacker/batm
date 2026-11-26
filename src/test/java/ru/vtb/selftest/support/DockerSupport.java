package ru.vtb.selftest.support;

import org.testcontainers.DockerClientFactory;
import org.testng.SkipException;

/**
 * Поддержка selftest-тестов, которым нужен Docker (Testcontainers).
 * <p>
 * Образы берутся со стандартного Docker Hub. Префикс реестра задаётся Maven-свойством
 * {@code selftest.registry} (например, {@code -Dselftest.registry=registry.example.com/dockerhub}),
 * слеш на конце необязателен. Префикс применяет {@link RegistryImageNameSubstitutor} ко всем образам
 * Docker Hub, включая служебный образ Testcontainers (ryuk).
 */
public final class DockerSupport {

    public static final String REGISTRY_PROPERTY = "selftest.registry";

    private DockerSupport() {
    }

    /**
     * Пропускает текущий тест (SkipException), если Docker недоступен.
     */
    public static void requireDocker() {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new SkipException("Docker недоступен: Testcontainers не нашёл работающий Docker daemon "
                    + "(DOCKER_HOST, /var/run/docker.sock или ~/.testcontainers.properties). "
                    + "Тест пропущен; для запуска нужен доступ к Docker.");
        }
    }

    /**
     * @return префикс реестра из свойства selftest.registry или пустая строка (Docker Hub)
     */
    public static String registryPrefix() {
        String registry = System.getProperty(REGISTRY_PROPERTY);
        return registry == null ? "" : registry.trim();
    }
}
