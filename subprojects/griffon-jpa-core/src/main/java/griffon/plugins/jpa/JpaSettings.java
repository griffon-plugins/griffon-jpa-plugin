/*
 * Copyright 2014-2015 the original author or authors.
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
package griffon.plugins.jpa;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public final class JpaSettings {
    private final EntityManagerFactory entityManagerFactory;
    private final EntityManager entityManager;

    public JpaSettings(@Nonnull EntityManagerFactory entityManagerFactory, @Nonnull EntityManager entityManager) {
        this.entityManagerFactory = requireNonNull(entityManagerFactory, "Argument 'entityManagerFactory' must not be null");
        this.entityManager = requireNonNull(entityManager, "Argument 'entityManager' must not be null");
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JpaSettings that = (JpaSettings) o;

        return entityManager.equals(that.entityManager) && entityManagerFactory.equals(that.entityManagerFactory);
    }

    @Override
    public int hashCode() {
        int result = entityManagerFactory.hashCode();
        result = 31 * result + entityManager.hashCode();
        return result;
    }
}
