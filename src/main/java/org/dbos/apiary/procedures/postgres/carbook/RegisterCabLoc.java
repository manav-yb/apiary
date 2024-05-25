package org.dbos.apiary.procedures.postgres.carbook;

import org.dbos.apiary.postgres.PostgresContext;
import org.dbos.apiary.postgres.PostgresFunction;

/*
 * CarStatus:
 * 0 --> Car not available.
 * 1 --> Car is available and looking for client.
 * 2 --> Car is booked and on a trip
 */

public class RegisterCabLoc extends PostgresFunction {

    private static final String insert = "INSERT INTO CarsTable(CarID, CarName, CarStatus) VALUES (?, ?, ?);";

    public static int runFunction(PostgresContext ctxt, int CarID, String CarName, int CarStatus, int longitude, int latitude)throws Exception {
        ctxt.executeUpdate(insert, CarID, CarName, CarStatus);
        // Using infrastructure for hotels to add/search cars
        ctxt.apiaryCallFunction("MongoAddHotel", CarID, longitude, latitude);
        return CarID;
    }
}
