package org.dbos.apiary.procedures.postgres.carbook;

import org.dbos.apiary.postgres.PostgresContext;
import org.dbos.apiary.postgres.PostgresFunction;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RequestCab extends PostgresFunction {

    // Select cars which are looking for trip
    private static final String get = "SELECT CARID FROM CarsTable WHERE CARID=? AND CARSTATUS == 1";

    public static int[] runFunction(PostgresContext ctxt, int longitude, int latitude)throws Exception {
        
        // Using infrastructure similar to hotels to add/search cars
        int[] cars = ctxt.apiaryCallFunction("MongoSearchHotel", longitude, latitude).getIntArray();
        List<Integer> availableHotels = new ArrayList<>();
        for (int carID : cars) {
            ResultSet rs = ctxt.executeQuery(get, carID);
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    availableHotels.add(carID);
                }
            }
        }
        return availableHotels.stream().mapToInt(i -> i).toArray();
    }
}
