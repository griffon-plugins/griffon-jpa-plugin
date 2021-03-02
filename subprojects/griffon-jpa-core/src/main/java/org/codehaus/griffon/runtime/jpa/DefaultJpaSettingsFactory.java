/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2021 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.jpa;

import griffon.annotations.core.Nonnull;
import griffon.core.Configuration;
import griffon.core.GriffonApplication;
import griffon.core.injection.Injector;
import griffon.plugins.jpa.JpaBootstrap;
import griffon.plugins.jpa.JpaSettings;
import griffon.plugins.jpa.JpaSettingsFactory;
import griffon.plugins.jpa.events.JpaConnectEndEvent;
import griffon.plugins.jpa.events.JpaConnectStartEvent;
import griffon.plugins.jpa.events.JpaDisconnectEndEvent;
import griffon.plugins.jpa.events.JpaDisconnectStartEvent;
import org.codehaus.griffon.runtime.core.storage.AbstractObjectFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static griffon.util.ConfigUtils.getConfigValue;
import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultJpaSettingsFactory extends AbstractObjectFactory<JpaSettings> implements JpaSettingsFactory {
    private static final String ERROR_PERSISTENCE_UNIT_NAME_BLANK = "Argument 'persistenceUnitName' must not be blank";

    private final Set<String> persistenceUnitNames = new LinkedHashSet<>();

    @Inject
    private Injector injector;

    @Inject
    public DefaultJpaSettingsFactory(@Nonnull @Named("jpa") Configuration configuration, @Nonnull GriffonApplication application) {
        super(configuration, application);
        persistenceUnitNames.add(KEY_DEFAULT);

        if (configuration.containsKey(getPluralKey())) {
            Map<String, Object> persistenceUnits = (Map<String, Object>) configuration.get(getPluralKey());
            persistenceUnitNames.addAll(persistenceUnits.keySet());
        }
    }

    @Nonnull
    @Override
    public Set<String> getPersistenceUnitNames() {
        return persistenceUnitNames;
    }

    @Nonnull
    @Override
    public Map<String, Object> getConfigurationFor(@Nonnull String persistenceUnitName) {
        requireNonBlank(persistenceUnitName, ERROR_PERSISTENCE_UNIT_NAME_BLANK);
        return narrowConfig(persistenceUnitName);
    }

    @Nonnull
    @Override
    protected String getSingleKey() {
        return "persistenceUnit";
    }

    @Nonnull
    @Override
    protected String getPluralKey() {
        return "persistenceUnits";
    }

    @Nonnull
    @Override
    public JpaSettings create(@Nonnull String name) {
        requireNonBlank(name, ERROR_PERSISTENCE_UNIT_NAME_BLANK);
        Map<String, Object> config = narrowConfig(name);

        event(JpaConnectStartEvent.of(name, config));

        JpaSettings jpaSettings = createJpaSettings(config, name);

        for (Object o : injector.getInstances(JpaBootstrap.class)) {
            ((JpaBootstrap) o).init(name, jpaSettings.getEntityManager());
        }

        event(JpaConnectEndEvent.of(name, config, jpaSettings.getEntityManager()));

        return jpaSettings;
    }

    @Override
    public void destroy(@Nonnull String name, @Nonnull JpaSettings instance) {
        requireNonBlank(name, ERROR_PERSISTENCE_UNIT_NAME_BLANK);
        requireNonNull(instance, "Argument 'instance' must not be null");
        Map<String, Object> config = narrowConfig(name);

        event(JpaDisconnectStartEvent.of(name, config, instance.getEntityManager()));

        for (Object o : injector.getInstances(JpaBootstrap.class)) {
            ((JpaBootstrap) o).destroy(name, instance.getEntityManager());
        }

        instance.getEntityManager().close();
        instance.getEntityManagerFactory().close();

        event(JpaDisconnectEndEvent.of(name, config));
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    private JpaSettings createJpaSettings(@Nonnull Map<String, Object> config, @Nonnull String name) {
        Map<String, Object> factoryProperties = getConfigValue(config, "factory", Collections.<String, Object>emptyMap());
        Map<String, Object> entityManagerProperties = getConfigValue(config, "entityManager", Collections.<String, Object>emptyMap());

        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(name, factoryProperties);
        EntityManager entityManager = entityManagerFactory.createEntityManager(entityManagerProperties);
        return new JpaSettings(entityManagerFactory, entityManager);
    }
}
