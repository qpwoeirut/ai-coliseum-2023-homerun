package h1_scouting;

import aic2023.user.*;
import h1_scouting.util.Util;

public class PitcherPlayer extends BasePlayer {
    PitcherPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        MapObject claimedObjectType = null;
        Location claimedObjectLocation = null;
        int claimedObjectId = -1;
        while (true) {
            comms.checkIn();
            senseAndReportBases();
            senseAndReportStadiums();

            final UnitInfo[] enemies = senseAndReportEnemies();
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), enemies, UnitType.BATTER);
            if (nearestEnemyBatter != null && Util.chebyshevDistance(uc.getLocation(), nearestEnemyBatter.getLocation()) <= 4 && comms.lowerBoundDistance(nearestEnemyBatter.getLocation()) <= comms.DISTANCE_UNIT * 5) {
                Util.tryMoveInDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()));
            }

//            uc.println("type, location, id: " + claimedObjectType + " " + claimedObjectLocation + " " + claimedObjectId);

            if (claimedObjectLocation != null) {
                if (claimedObjectType == MapObject.BASE) {
                    comms.updateClaimOnBase(claimedObjectId);
                } else {
                    comms.updateClaimOnStadium(claimedObjectId);
                }
                if (uc.canMove() && !claimedObjectLocation.isEqual(uc.getLocation())) {
                    Direction toMove = comms.directionViaFocalPoint(claimedObjectLocation);
                    if (toMove == null) toMove = bg.move(claimedObjectLocation);
                    if (uc.canMove(toMove) && toMove != Direction.ZERO) {
                        uc.move(toMove);
                    }
                }
            } else {
                final int unclaimedStadiumCount = comms.listUnclaimedStadiums();
//                uc.println("unclaimedStadiumCount: " + unclaimedStadiumCount);
                final int claimedStadiumIndex = Util.getNearestIndex(uc.getLocation(), comms.returnedLocations, unclaimedStadiumCount);
                if (claimedStadiumIndex != -1) {
                    claimedObjectType = MapObject.STADIUM;
                    claimedObjectLocation = comms.returnedLocations[claimedStadiumIndex];
                    claimedObjectId = comms.returnedIds[claimedStadiumIndex];
                    comms.claimStadium(claimedObjectId);
                }

                if (claimedObjectLocation == null) {
                    final int unclaimedBaseCount = comms.listUnclaimedBases();
//                    uc.println("unclaimedBaseCount: " + unclaimedBaseCount);
//                    for (int i = 0; i < unclaimedBaseCount; ++i) {
//                        uc.println(comms.returnedLocations[i] + " " + comms.returnedIds[i]);
//                    }
                    final int claimedBaseIndex = Util.getNearestIndex(uc.getLocation(), comms.returnedLocations, unclaimedBaseCount);
                    if (claimedBaseIndex != -1) {
                        claimedObjectType = MapObject.BASE;
                        claimedObjectLocation = comms.returnedLocations[claimedBaseIndex];
                        claimedObjectId = comms.returnedIds[claimedBaseIndex];
                        comms.claimBase(claimedObjectId);
                    }
                }
            }

            endTurn();
        }
    }
}
