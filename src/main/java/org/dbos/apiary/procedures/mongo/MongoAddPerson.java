package org.dbos.apiary.procedures.mongo;

import org.bson.Document;
import org.dbos.apiary.mongo.MongoContext;
import org.dbos.apiary.mongo.MongoFunction;

public class MongoAddPerson extends MongoFunction {

    public int runFunction(MongoContext context, String name, int number) {
        Document person = new Document("name", name).append("number", number);
        context.insertOne("people", person, name);
        return number;
    }
}