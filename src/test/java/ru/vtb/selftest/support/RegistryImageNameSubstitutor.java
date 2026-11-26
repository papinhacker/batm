package ru.vtb.selftest.support;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * Добавляет префикс реестра из свойства selftest.registry к образам Docker Hub
 * (включая служебный образ Testcontainers). Образы с явно указанным реестром не меняются.
 * Подключается профилем selftest через переменную окружения TESTCONTAINERS_IMAGE_SUBSTITUTOR.
 */
public class RegistryImageNameSubstitutor extends ImageNameSubstitutor {

    @Override
    public DockerImageName apply(DockerImageName original) {
        String prefix = DockerSupport.registryPrefix();
        if (prefix.isEmpty() || !original.getRegistry().isEmpty()) {
            return original;
        }
        String normalized = prefix.endsWith("/") ? prefix : prefix + "/";
        return DockerImageName.parse(normalized + original.asCanonicalNameString().replaceFirst("^docker\\.io/", ""))
                .asCompatibleSubstituteFor(original);
    }

    @Override
    protected String getDescription() {
        return "selftest.registry prefix substitutor";
    }
}
