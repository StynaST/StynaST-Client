package net.styna.stynastmc.platform;

import java.util.ServiceLoader;

final class Services {
    private Services() {
    }

    static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No implementation of " + type.getName()));
    }
}
