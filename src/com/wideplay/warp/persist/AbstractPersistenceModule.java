/**
 * Copyright (C) 2008 Wideplay Interactive.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wideplay.warp.persist;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.cglib.proxy.Proxy;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import com.wideplay.warp.persist.dao.Finder;
import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Base module for persistence strategies that holds a bunch
 * of utility methods for easier configuration.
 * @author Robbie Vanbrabant
 */
public abstract class AbstractPersistenceModule extends AbstractModule implements PersistenceModule {
    private final Class<? extends Annotation> annotation;

    protected AbstractPersistenceModule(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    protected abstract void configure();

    /**
     * Bind with an optional binding annotation type. A binding annotation needs
     * to be specified when using two Hibernate configurations within the same Injector.
     */
    protected <T> com.google.inject.binder.LinkedBindingBuilder<T> bindSpecial(java.lang.Class<T> tClass) {
        if (inMultiModulesMode()) {
            return super.bind(tClass).annotatedWith(annotation);
        } else {
            return super.bind(tClass);
        }
    }

    @SuppressWarnings("unchecked") // Proxies are not generic.
    protected void bindDynamicAccessors(Set<Class<?>> accessors, MethodInterceptor finderInterceptor) {
        for (Class accessor : accessors) {
            if (accessor.isInterface()) {
                // TODO we should validate that all methods have @Finder on them at startup
                //      and use Guice's addError.
                bindSpecial(accessor).toInstance(Proxy.newProxyInstance(accessor.getClassLoader(),
                        new Class<?>[] { accessor }, new AopAllianceJdkProxyAdapter(finderInterceptor)));
            } else {
                //use cglib adapter to subclass the accessor (this lets us intercept abstract classes)
                bindSpecial(accessor).toInstance(com.google.inject.cglib.proxy.Enhancer.create(accessor,
                        new AopAllianceCglibAdapter(finderInterceptor)));
            }
        }
    }

    protected void bindTransactionInterceptor(PersistenceConfiguration config, MethodInterceptor txInterceptor) {
        if (inMultiModulesMode()) {
            // We support forAll, and assume the user knows what he/she is doing.
            // TODO make our custom method matcher public?
            if (config.getTransactionMethodMatcher() != Defaults.TX_METHOD_MATCHER) {
                bindInterceptor(config.getTransactionClassMatcher(),
                                config.getTransactionMethodMatcher(),
                                txInterceptor);
            } else {
                bindInterceptor(config.getTransactionClassMatcher(),
                                transactionalWithUnitIdenticalTo(annotation),
                                txInterceptor);
            }
        } else {
            bindInterceptor(config.getTransactionClassMatcher(), config.getTransactionMethodMatcher(), txInterceptor);
        }
    }

    /**
     * Binds a finder interceptor with support for multiple modules. When the user specifies
     * an annotation to bind the module to, we match on {@code @Finder(unit=UserAnnotation.class)}.
     */
    protected void bindFinderInterceptor(MethodInterceptor finderInterceptor) {
        if (inMultiModulesMode()) {
            bindInterceptor(any(), finderWithUnitIdenticalTo(annotation), finderInterceptor);
        } else {
            bindInterceptor(any(), annotatedWith(Finder.class), finderInterceptor);
        }
    }

    protected boolean inMultiModulesMode() {
        return this.annotation != null;
    }

    protected <T> Key<T> key(Class<T> clazz) {
        if (annotation != null) {
            return Key.get(clazz, annotation);
        }
        return Key.get(clazz);
    }

    private Matcher<Method> finderWithUnitIdenticalTo(final Class<?> annotation) {
        return new AbstractMatcher<Method>() {
            public boolean matches(Method method) {
                return annotatedWith(Finder.class).matches(method) &&
                       method.getAnnotation(Finder.class).unit() == annotation;
            }
        };
    }

    private Matcher<? super Method> transactionalWithUnitIdenticalTo(final Class<?> annotation) {
        return new AbstractMatcher<Method>() {
            public boolean matches(Method method) {
                return annotatedWith(Transactional.class).matches(method) &&
                       method.getAnnotation(Transactional.class).unit() == annotation;
            }
        };
    }
}
