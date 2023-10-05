package h4_queuescouting;

import aic2023.user.*;
import h4_queuescouting.util.Util;

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
            senseAndReportEnemies();

            final UnitInfo[] nearbyEnemies = uc.senseUnits(REACHABLE_VISION, uc.getOpponent());
            final int directionOkay = calculateOkayDirections(nearbyEnemies);
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), nearbyEnemies, UnitType.BATTER);
//            debug("directionOkay = " + directionOkay);
            if (nearestEnemyBatter != null && ((directionOkay >> Direction.ZERO.ordinal()) & 1) > 0) {
//                debug("moving away from " + nearestEnemyBatter.getLocation());
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
                    if (toMove == null) toMove = bg.move(claimedObjectLocation);
                    if (toMove != null && toMove != Direction.ZERO && uc.canMove(toMove) && ((directionOkay >> toMove.ordinal()) & 1) > 0) {
                        uc.move(toMove);
                    } else {
                        Util.tryMoveInOkayDirection(uc, uc.getLocation().directionTo(claimedObjectLocation), directionOkay);
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
