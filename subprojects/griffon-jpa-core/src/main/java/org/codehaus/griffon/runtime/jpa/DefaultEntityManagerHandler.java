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

import griffon.plugins.jpa.EntityManagerCallback;
import griffon.plugins.jpa.JpaSettingsFactory;
import griffon.plugins.jpa.EntityManagerHandler;
import griffon.plugins.jpa.JpaSettings;
import griffon.plugins.jpa.JpaSettingsStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import griffon.annotations.core.Nonnull;
import griffon.annotations.core.Nullable;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultEntityManagerHandler implements EntityManagerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityManagerHandler.class);
    private static final String ERROR_DATASBASE_BLANK = "Argument 'persistenceUnitName' must not be blank";
    private static final String ERROR_CONNECTION_SOURCE_NULL = "Argument 'entityManager' must not be null";
    private static final String ERROR_CALLBACK_NULL = "Argument 'callback' must not be null";

    private final JpaSettingsFactory jpaSettingsFactory;
    private final JpaSettingsStorage jpaSettingsStorage;

    @Inject
    public DefaultEntityManagerHandler(@Nonnull JpaSettingsFactory jpaSettingsFactory, @Nonnull JpaSettingsStorage jpaSettingsStorage) {
        this.jpaSettingsFactory = requireNonNull(jpaSettingsFactory, "Argument 'entityManagerFactory' must not be null");
        this.jpaSettingsStorage = requireNonNull(jpaSettingsStorage, "Argument 'entityManagerStorage' must not be null");
    }

    @Nullable
    @Override
    public <R> R withEntityManager(@Nonnull EntityManagerCallback<R> callback) {
        return withEntityManager(DefaultJpaSettingsFactory.KEY_DEFAULT, callback);
    }

    @Nullable
    @Override
    public <R> R withEntityManager(@Nonnull String persistenceUnitName, @Nonnull EntityManagerCallback<R> callback) {
        requireNonBlank(persistenceUnitName, ERROR_DATASBASE_BLANK);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        EntityManager entityManager = getEntityManager(persistenceUnitName);
        return doWithConnection(persistenceUnitName, entityManager, callback);
    }

    @Nullable
    @SuppressWarnings("ThrowFromFinallyBlock")
    static <R> R doWithConnection(@Nonnull String persistenceUnitName, @Nonnull EntityManager entityManager, @Nonnull EntityManagerCallback<R> callback) {
        requireNonBlank(persistenceUnitName, ERROR_DATASBASE_BLANK);
        requireNonNull(entityManager, ERROR_CONNECTION_SOURCE_NULL);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        LOG.debug("Executing statements on database '{}'", persistenceUnitName);
        return callback.handle(persistenceUnitName, entityManager);
    }

    @Override
    public void closeEntityManager() {
        closeEntityManager(DefaultJpaSettingsFactory.KEY_DEFAULT);
    }

    @Override
    public void closeEntityManager(@Nonnull String persistenceUnitName) {
        JpaSettings japSettings = jpaSettingsStorage.get(persistenceUnitName);
        if (japSettings != null) {
            jpaSettingsFactory.destroy(persistenceUnitName, japSettings);
            jpaSettingsStorage.remove(persistenceUnitName);
        }
    }

    @Nonnull
    private EntityManager getEntityManager(@Nonnull String persistenceUnitName) {
        JpaSettings japSettings = jpaSettingsStorage.get(persistenceUnitName);
        if (japSettings == null) {
            japSettings = jpaSettingsFactory.create(persistenceUnitName);
            jpaSettingsStorage.set(persistenceUnitName, japSettings);
        }
        return japSettings.getEntityManager();
    }
}
