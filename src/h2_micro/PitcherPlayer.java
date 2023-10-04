package h2_micro;

import aic2023.user.*;
import h2_micro.util.Util;

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
            final int directionOkay = calculateOkayDirections(enemies);
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), enemies, UnitType.BATTER);
//            debug("directionOkay = " + directionOkay);
            if (nearestEnemyBatter != null && enemyBatterCanHitLocation(uc.getInfo().getCurrentMovementCooldown(), uc.getLocation(), enemies)) {
                Util.tryMoveInOkayDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()), directionOkay);
            }

//            uc.println("type, location, id: " + claimedObjectType + " " + claimedObjectLocation + " " + claimedObjectId);

            if (claimedObjectLocation != null) {
                if (claimedObjectType == MapObject.BASE) {
                    comms.updateClaimOnBase(claimedObjectId);
                } else {
                    comms.updateClaimOnStadium(claimedObjectId);
                }
                if (uc.canMove() && !claimedObjectLocation.isEqual(uc.getLocation())) {
                    Direction toMove = comms.directionViaFocalPoint(claimedObjectLocation, directionOkay);
                    if (toMove == null) toMove = uc.getLocation().directionTo(claimedObjectLocation);
                    if (toMove != Direction.ZERO) {
                        Util.tryMoveInOkayDirection(uc, toMove, directionOkay);
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
