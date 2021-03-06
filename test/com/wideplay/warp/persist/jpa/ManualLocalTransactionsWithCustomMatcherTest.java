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

package com.wideplay.warp.persist.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;
import com.wideplay.warp.persist.PersistenceService;
import com.wideplay.warp.persist.UnitOfWork;
import com.wideplay.warp.persist.WorkManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * On: 2/06/2007
 *
 * For instance, a session-per-request strategy will control the opening and closing of the EM
 * at its own (manual) discretion. As opposed to a transactional unit of work.
 *
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 * @since 1.0
 */
public class ManualLocalTransactionsWithCustomMatcherTest {
    private Injector injector;
    private static final String UNIQUE_TEXT = "some unique text" + new Date();
    private static final String UNIQUE_TEXT_2 = "some other unique text" + new Date();

    @BeforeClass
    public void pre() {
        injector = Guice.createInjector(PersistenceService.usingJpa()
            .across(UnitOfWork.REQUEST)
            .forAll(Matchers.subclassesOf(TransactionalObject.class), Matchers.any())
            .buildModule(),
                new AbstractModule() {

                    protected void configure() {
                        //tell Warp the name of the jpa persistence unit
                        bindConstant().annotatedWith(JpaUnit.class).to("testUnit");
                    }
                });

        //startup persistence
        injector.getInstance(PersistenceService.class)
                .start();
    }

    @AfterClass
    public void postClass() {
        injector.getInstance(EntityManagerFactory.class).close();
    }

    @Test
    public void testSimpleCrossTxnWork() {
        //pretend that the request was started here
        EntityManager em = injector.getInstance(EntityManager.class);

        JpaTestEntity entity = injector.getInstance(ManualLocalTransactionsWithCustomMatcherTest.TransactionalObject.class).runOperationInTxn();
        injector.getInstance(ManualLocalTransactionsWithCustomMatcherTest.TransactionalObject.class).runOperationInTxn2();

        //persisted entity should remain in the same em (which should still be open)
        assert injector.getInstance(EntityManager.class).contains(entity) : "EntityManager  appears to have been closed across txns!";
        assert em.contains(entity) : "EntityManager  appears to have been closed across txns!";
        assert em.isOpen() : "EntityManager appears to have been closed across txns!";

        injector.getInstance(WorkManager.class).endWork();

        //try to query them back out
        em = injector.getInstance(EntityManager.class);
        assert null != em.createQuery("from JpaTestEntity where text = :text").setParameter("text", UNIQUE_TEXT).getSingleResult();
        assert null != em.createQuery("from JpaTestEntity where text = :text").setParameter("text", UNIQUE_TEXT_2).getSingleResult();
        em.close();
    }


    public static class TransactionalObject {
        @Inject
        EntityManager em;

        public JpaTestEntity runOperationInTxn() {
            JpaTestEntity entity = new JpaTestEntity();
            entity.setText(UNIQUE_TEXT);
            em.persist(entity);

            return entity;
        }

        public void runOperationInTxn2() {
            JpaTestEntity entity = new JpaTestEntity();
            entity.setText(UNIQUE_TEXT_2);
            em.persist(entity);
        }

    }
}
