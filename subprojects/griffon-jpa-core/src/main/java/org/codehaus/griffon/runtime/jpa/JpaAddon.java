/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.jpa;

import griffon.core.GriffonApplication;
import griffon.core.env.Metadata;
import griffon.plugins.jpa.EntityManagerCallback;
import griffon.plugins.jpa.JpaSettingsFactory;
import griffon.plugins.jpa.EntityManagerHandler;
import griffon.plugins.jpa.JpaSettingsStorage;
import griffon.plugins.monitor.MBeanManager;
import org.codehaus.griffon.runtime.core.addon.AbstractGriffonAddon;
import org.codehaus.griffon.runtime.jmx.JpaSettingsStorageMonitor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import java.util.Map;

import static griffon.util.ConfigUtils.getConfigValueAsBoolean;

/**
 * @author Andres Almiray
 */
@Named("jpa")
public class JpaAddon extends AbstractGriffonAddon {
    @Inject
    private EntityManagerHandler entityManagerHandler;

    @Inject
    private JpaSettingsFactory jpaSettingsFactory;

    @Inject
    private JpaSettingsStorage jpaSettingsStorage;

    @Inject
    private MBeanManager mbeanManager;

    @Inject
    private Metadata metadata;

    @Override
    public void init(@Nonnull GriffonApplication application) {
        mbeanManager.registerMBean(new JpaSettingsStorageMonitor(metadata, jpaSettingsStorage));
    }

    public void onStartupStart(@Nonnull GriffonApplication application) {
        for (String persistenceUnitName : jpaSettingsFactory.getPersistenceUnitNames()) {
            Map<String, Object> config = jpaSettingsFactory.getConfigurationFor(persistenceUnitName);
            if (getConfigValueAsBoolean(config, "connect_on_startup", false)) {
                entityManagerHandler.withEntityManager(new EntityManagerCallback<Object>() {
                    @Override
                    public Object handle(@Nonnull String persistenceUnitName, @Nonnull EntityManager entityManager) {
                        return null;
                    }
                });
            }
        }
    }

    public void onShutdownStart(@Nonnull GriffonApplication application) {
        for (String persistenceUnitName : jpaSettingsFactory.getPersistenceUnitNames()) {
            entityManagerHandler.closeEntityManager(persistenceUnitName);
        }
    }
}
