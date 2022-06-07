package org.dbos.apiary.connection;

import org.dbos.apiary.function.FunctionOutput;
import org.dbos.apiary.function.TransactionContext;
import org.dbos.apiary.function.WorkerContext;

import java.util.List;
import java.util.Set;

/**
 * A connection to a secondary database.
 */
public interface ApiarySecondaryConnection {

    FunctionOutput callFunction(String functionName, WorkerContext workerContext, TransactionContext transactionContext, String service, long execID, long functionID, Object... inputs) throws Exception;

    boolean validate(List<String> updatedKeys, TransactionContext txc);

    void garbageCollect(Set<TransactionContext> activeTransactions);
}