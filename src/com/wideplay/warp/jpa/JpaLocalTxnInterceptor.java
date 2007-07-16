package com.wideplay.warp.jpa;

import com.wideplay.warp.persist.Transactional;
import com.wideplay.warp.persist.UnitOfWork;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * Created with IntelliJ IDEA.
 * On: May 26, 2007 3:07:46 PM
 *
 * @author Dhanji R. Prasanna <a href="mailto:dhanji@gmail.com">email</a>
 */
class JpaLocalTxnInterceptor implements MethodInterceptor {
    private static UnitOfWork unitOfWork;

    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        EntityManager em = EntityManagerFactoryHolder.getCurrentEntityManager();

        //start txn
        final EntityTransaction txn = em.getTransaction();
        txn.begin();

        Object result;
        try {
            result = methodInvocation.proceed();

        } catch (Exception e) {
            Transactional transactional = methodInvocation.getMethod().getAnnotation(Transactional.class);

            //commit transaction only if rollback didnt occur
            if (rollbackIfNecessary(transactional, e, txn))
                txn.commit();

            //propagate whatever exception is thrown anyway
            throw e;
        } finally {
            //close the em if necessary
            if (isUnitOfWorkTransaction()) {
                EntityManagerFactoryHolder.closeCurrentEntityManager();
            }
        }


        //everything was normal so commit the txn (do not move into try block as it interferes with the advised method's throwing semantics)
        txn.commit();

        //or return result
        return result;
    }

    /**
     *
     * @param transactional The metadata annotaiton of the method
     * @param e The exception to test for rollback
     * @param txn A Hibernate Transaction to issue rollbacks against
     * @return returns Returns true if rollback DID NOT HAPPEN (i.e. if commit should continue)
     */
    private boolean rollbackIfNecessary(Transactional transactional, Exception e, EntityTransaction txn) {
        boolean commit = true;

        //check rollback clauses
        for (Class<? extends Exception> rollBackOn : transactional.rollbackOn()) {

            //if one matched, try to perform a rollback
            if (rollBackOn.isInstance(e)) {
                commit = false;

                //check exceptOn clauses (supercedes rollback clause)
                for (Class<? extends Exception> exceptOn : transactional.exceptOn()) {

                    //An exception to the rollback clause was found, DONT rollback (i.e. commit and throw anyway)
                    if (exceptOn.isInstance(e)) {
                        commit = true;
                        break;
                    }
                }

                //rollback only if nothing matched the exceptOn check
                if (!commit) {
                    txn.rollback();
                }
                //otherwise continue to commit

                break;
            }
        }

        return commit;
    }

    private static boolean isUnitOfWorkTransaction() {
        return UnitOfWork.TRANSACTION.equals(unitOfWork);
    }


    static void setUnitOfWork(UnitOfWork unitOfWork) {
        JpaLocalTxnInterceptor.unitOfWork = unitOfWork;
    }
}
