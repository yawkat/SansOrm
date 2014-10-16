/*
 Copyright 2012, Brett Wooldridge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.zaxxer.sansorm;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionElf
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionElf.class);

    private static TransactionManager transactionManager;
    private static UserTransaction userTransaction;

    /**
     * Set the JTA TransactionManager implementation used by the Elf.
     *
     * @param tm a JTA TransactionManager instance 
     */
    public static void setTransactionManager(TransactionManager tm)
    {
        transactionManager = tm;
    }

    public static void setUserTransaction(UserTransaction ut)
    {
        userTransaction = ut;
    }

    /**
     * Returns true if a JTA transaction manager is registered, false
     * otherwise.
     *
     * @return true if a JTA transaction manager is registered
     */
    public static boolean hasTransactionManager()
    {
        return transactionManager != null;
    }

    /**
     * Start or join a transaction.
     *
     * @return true if a new transaction was started (this means the caller "owns"
     *    the commit()), false if a transaction was joined.
     */
    public static boolean beginOrJoinTransaction()
    {
        if (userTransaction == null) {
            return true;
        }

        boolean newTransaction = false;
        try
        {
            newTransaction = userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION;
            if (newTransaction)
            {
                userTransaction.begin();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to start transaction.", e);
        }

        return newTransaction;
    }

    /**
     * Commit the current transaction.
     */
    public static void commit()
    {
        if (userTransaction == null) {
            return;
        }

        try
        {
            int status = userTransaction.getStatus();
            if (status != Status.STATUS_ROLLING_BACK
                && status != Status.STATUS_MARKED_ROLLBACK
                && status != Status.STATUS_ROLLEDBACK
                && status != Status.STATUS_ROLLING_BACK
                && status != Status.STATUS_UNKNOWN
                && status != Status.STATUS_NO_TRANSACTION
                && status != Status.STATUS_COMMITTED)
            {
                userTransaction.commit();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Transaction commit failed.", e);
        }
    }

    /**
     * Rollback the current transaction.
     */
    public static void rollback()
    {
        try
        {
            if (userTransaction.getStatus() == Status.STATUS_ACTIVE)
            {
                userTransaction.rollback();
            }
            else
            {
                LOGGER.warn("Request to rollback transaction when none was in active.");
            }
        }
        catch (Exception e)
        {
             LOGGER.warn("Transaction rollback failed.", e);
        }
    }

    /**
     * Suspend the current transaction and return it to the caller.
     *
     * @return the suspended Transaction
     */
    public static Transaction suspend()
    {
        try
        {
            Transaction suspend = transactionManager.suspend();
            return suspend;
        }
        catch (SystemException e)
        {
            throw new RuntimeException("Unable to suspend current transaction", e);
        }
    }

    /**
     * Resume the specified transaction.  If the transaction was never suspended, or was
     * already committed or rolled back, a RuntimeException will be thrown wrapping the
     * JTA originated exception.
     *
     * @param transaction the Transaction to resume
     */
    public static void resume(Transaction transaction)
    {
        try
        {
            transactionManager.resume(transaction);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to resume transaction", e);
        }
    }
}
