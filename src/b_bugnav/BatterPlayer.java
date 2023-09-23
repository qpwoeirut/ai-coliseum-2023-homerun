package b_bugnav;
import aic2023.user.*;
import b_bugnav.util.FastRandom;

import java.util.*;
public class BatterPlayer {
    private final UnitController uc;
    private final FastRandom random;
    BatterPlayer(UnitController uc) {
        this.uc = uc;
        this.random = new FastRandom(uc.getInfo().getID());
    }

    void run() {


        Team my = uc.getTeam();
        while (true) {
            Location loc = uc.getLocation();
            if (uc.canAct()){
                for (int x = -1; x <=1 ; x++){
                    for (int y = -1; y <= 1; y++){
                        Location dir = loc.add(x,y);

                        UnitInfo u = uc.senseUnitAtLocation(dir);
                        if (u.equals(null)){
                            continue;
                        }
                        //UnitType t = u.getType();
                        else{
                            uc.bat(loc.directionTo(dir), 3);
                            uc.yield();
                        }
                    }
                }

            }
            uc.yield();
        }
    }
}
