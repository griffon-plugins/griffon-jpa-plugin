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
package griffon.plugins.jpa

import griffon.annotations.inject.BindTo
import griffon.core.GriffonApplication
import griffon.plugins.jpa.events.JpaConnectEndEvent
import griffon.plugins.jpa.events.JpaConnectStartEvent
import griffon.plugins.jpa.events.JpaDisconnectEndEvent
import griffon.plugins.jpa.events.JpaDisconnectStartEvent
import griffon.test.core.GriffonUnitRule
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import javax.application.event.EventHandler
import javax.inject.Inject
import javax.persistence.EntityManager
import javax.persistence.PersistenceException

@Unroll
class JpaSpec extends Specification {
    static {
        System.setProperty('org.slf4j.simpleLogger.defaultLogLevel', 'trace')
    }

    @Rule
    public final GriffonUnitRule griffon = new GriffonUnitRule()

    @Inject
    private EntityManagerHandler entityManagerHandler

    @Inject
    private GriffonApplication application

    void 'Open and close default entityManager'() {
        given:
        List eventNames = [
            'JpaConnectStartEvent', 'JpaConnectEndEvent',
            'JpaDisconnectStartEvent', 'JpaDisconnectEndEvent'
        ]
        TestEventHandler testEventHandler = new TestEventHandler()
        application.eventRouter.subscribe(testEventHandler)

        when:
        entityManagerHandler.withEntityManager { String persistenceUnitName, EntityManager entityManager ->
            true
        }
        entityManagerHandler.closeEntityManager()
        // second call should be a NOOP
        entityManagerHandler.closeEntityManager()

        then:
        testEventHandler.events.size() == 4
        testEventHandler.events == eventNames
    }

    void 'Connect to default entityManager'() {
        expect:
        entityManagerHandler.withEntityManager { String persistenceUnitName, EntityManager entityManager ->
            persistenceUnitName == 'default' && entityManager
        }
    }

    void 'Bootstrap init is called'() {
        given:
        assert !bootstrap.initWitness

        when:
        entityManagerHandler.withEntityManager { String persistenceUnitName, EntityManager entityManager -> }

        then:
        bootstrap.initWitness
        !bootstrap.destroyWitness
    }

    void 'Bootstrap destroy is called'() {
        given:
        assert !bootstrap.initWitness
        assert !bootstrap.destroyWitness

        when:
        entityManagerHandler.withEntityManager { String persistenceUnitName, EntityManager entityManager -> }
        entityManagerHandler.closeEntityManager()

        then:
        bootstrap.initWitness
        bootstrap.destroyWitness
    }

    void 'Can connect to #name entityManager'() {
        expect:
        entityManagerHandler.withEntityManager(name) { String persistenceUnitName, EntityManager entityManager ->
            persistenceUnitName == name && entityManager
        }

        where:
        name       | _
        'default'  | _
        'internal' | _
        'people'   | _
    }

    void 'Invalid entityManager name (#name) results in error'() {
        when:
        entityManagerHandler.withEntityManager(name) { String persistenceUnitName, EntityManager entityManager ->
            true
        }

        then:
        thrown(IllegalArgumentException)

        where:
        name | _
        null | _
        ''   | _
    }

    void 'Bogus entityManager name (#name) results in error'() {
        when:
        entityManagerHandler.withEntityManager(name) { String persistenceUnitName, EntityManager entityManager ->
            true
        }

        then:
        thrown(PersistenceException)

        where:
        name    | _
        'bogus' | _
    }

    void 'Execute statements on people entityManager'() {
        when:
        List peopleIn = entityManagerHandler.withEntityManager('people') { String persistenceUnitName, EntityManager entityManager ->
            List<Person> people = []
            entityManager.getTransaction().begin()
            [[id: 1, name: 'Danno', lastname: 'Ferrin'],
             [id: 2, name: 'Andres', lastname: 'Almiray'],
             [id: 3, name: 'James', lastname: 'Williams'],
             [id: 4, name: 'Guillaume', lastname: 'Laforge'],
             [id: 5, name: 'Jim', lastname: 'Shingler'],
             [id: 6, name: 'Alexander', lastname: 'Klein'],
             [id: 7, name: 'Rene', lastname: 'Groeschke']].each { data ->
                people << new Person(data)
                entityManager.persist(people.last())
            }
            entityManager.getTransaction().commit()
            people*.asMap()
        }

        List peopleOut = entityManagerHandler.withEntityManager('people') { String persistenceUnitName, EntityManager entityManager ->
            entityManager.createQuery('select p from Person p').resultList.collect([]) { it.asMap() }
        }

        then:
        peopleIn == peopleOut
    }

    @BindTo(JpaBootstrap)
    private TestJpaBootstrap bootstrap = new TestJpaBootstrap()

    private class TestEventHandler {
        List<String> events = []

        @EventHandler
        void handleJpaConnectStartEvent(JpaConnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleJpaConnectEndEvent(JpaConnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleJpaDisconnectStartEvent(JpaDisconnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleJpaDisconnectEndEvent(JpaDisconnectEndEvent event) {
            events << event.class.simpleName
        }
    }
}
