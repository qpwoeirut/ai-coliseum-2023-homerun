package b_bugnav;

import aic2023.user.Location;
import aic2023.user.Team;
import aic2023.user.UnitController;
import aic2023.user.UnitInfo;

public class BatterPlayer {
    private final UnitController uc;
    BatterPlayer(UnitController uc) {
        this.uc = uc;
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
                        if (u == null) {
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
