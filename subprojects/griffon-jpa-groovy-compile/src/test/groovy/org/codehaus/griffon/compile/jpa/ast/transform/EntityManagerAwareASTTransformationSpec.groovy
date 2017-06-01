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
package org.codehaus.griffon.compile.jpa.ast.transform

import griffon.plugins.jpa.EntityManagerHandler
import spock.lang.Specification

import java.lang.reflect.Method

/**
 * @author Andres Almiray
 */
class EntityManagerAwareASTTransformationSpec extends Specification {
    def 'EntityManagerAwareASTTransformation is applied to a bean via @EntityManagerAware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''import griffon.transform.EntityManagerAware
        @EntityManagerAware
        class Bean { }
        new Bean()
        ''')

        then:
        bean instanceof EntityManagerHandler
        EntityManagerHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }

    def 'EntityManagerAwareASTTransformation is not applied to a EntityManagerHandler subclass via @EntityManagerAware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''
        import griffon.plugins.jpa.EntityManagerCallback
        import griffon.plugins.jpa.EntityManagerHandler
        import griffon.transform.EntityManagerAware

        import javax.annotation.Nonnull
        @EntityManagerAware
        class EntityManagerHandlerBean implements EntityManagerHandler {
            @Override
            public <R> R withEntityManager(@Nonnull EntityManagerCallback<R> callback)  {
                return null
            }
            @Override
            public <R> R withEntityManager(@Nonnull String persistenceUnitName, @Nonnull EntityManagerCallback<R> callback)  {
                 return null
            }
            @Override
            void closeEntityManager(){}
            @Override
            void closeEntityManager(@Nonnull String persistenceUnitName){}
        }
        new EntityManagerHandlerBean()
        ''')

        then:
        bean instanceof EntityManagerHandler
        EntityManagerHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }
}
