package b_bugnav;

import aic2023.user.Direction;
import aic2023.user.Location;
import aic2023.user.MapObject;
import aic2023.user.UnitController;
import b_bugnav.util.Communications;
import b_bugnav.util.Util;

public class PitcherPlayer {
    private final UnitController uc;
    private final Communications comms;
    private final BugMover bg;

    PitcherPlayer(UnitController uc) {
        this.uc = uc;
        this.comms = new Communications(uc);
        this.bg = new BugMover(uc);
    }

    void run() {
        MapObject claimedObjectType = null;
        Location claimedObjectLocation = null;
        int claimedObjectId = -1;
        while (true) {
            comms.checkIn();

//            uc.println("type, location, id: " + claimedObjectType + " " + claimedObjectLocation + " " + claimedObjectId);

            if (claimedObjectLocation != null) {
                if (claimedObjectType == MapObject.BASE) {
                    comms.updateClaimOnBase(claimedObjectId);
                } else {
                    comms.updateClaimOnStadium(claimedObjectId);
                }
                Direction toMove = bg.move(claimedObjectLocation);
                if (uc.canMove(toMove)) {
                    uc.move(toMove);
                }
            } else {
                final int unclaimedBaseCount = comms.findUnclaimedBases();
//                uc.println("unclaimedBaseCount: " + unclaimedBaseCount);
//                for (int i = 0; i < unclaimedBaseCount; ++i) {
//                    uc.println(comms.returnedLocations[i] + " " + comms.returnedIds[i]);
//                }
                final int claimedBaseIndex = Util.getNearestIndex(uc.getLocation(), comms.returnedLocations, unclaimedBaseCount);
                if (claimedBaseIndex != -1) {
                    claimedObjectType = MapObject.BASE;
                    claimedObjectLocation = comms.returnedLocations[claimedBaseIndex];
                    claimedObjectId = comms.returnedIds[claimedBaseIndex];
                    comms.claimBase(claimedObjectId);
                }

                if (claimedObjectLocation == null) {
                    final int unclaimedStadiumCount = comms.findUnclaimedStadiums();
//                    uc.println("unclaimedStadiumCount: " + unclaimedStadiumCount);
                    final int claimedStadiumIndex = Util.getNearestIndex(uc.getLocation(), comms.returnedLocations, unclaimedStadiumCount);
                    if (claimedStadiumIndex != -1) {
                        claimedObjectType = MapObject.STADIUM;
                        claimedObjectLocation = comms.returnedLocations[claimedStadiumIndex];
                        claimedObjectId = comms.returnedIds[claimedStadiumIndex];
                        comms.claimStadium(claimedObjectId);
                    }
                }
            }

            uc.yield();
        }
    }
}
