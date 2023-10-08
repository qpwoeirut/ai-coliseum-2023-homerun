package p_final;

import aic2023.user.*;
import p_final.util.Util;

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

//            debugBytecode("after reporting objects");
            final UnitInfo[] nearbyEnemies = uc.senseUnits(REACHABLE_VISION, uc.getOpponent());
            final int directionOkay = calculateOkayDirections(nearbyEnemies);
//            debugBytecode("after directionOkay");
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), nearbyEnemies, UnitType.BATTER);
//            debug("directionOkay = " + directionOkay);
            if (nearestEnemyBatter != null && ((directionOkay >> Direction.ZERO.ordinal()) & 1) == 0) {
//                debug("moving away from " + nearestEnemyBatter.getLocation());
                Util.tryMoveInOkayDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()), directionOkay);
            }
//            debugBytecode("after run away");

//            uc.println("type, location, id: " + claimedObjectType + " " + claimedObjectLocation + " " + claimedObjectId);
            if (claimedObjectLocation == null) {
                final int unclaimedStadium = comms.nearestUnclaimedStadiumAsPitcher();
                if (unclaimedStadium != -1) {
                    claimedObjectType = MapObject.STADIUM;
                    claimedObjectLocation = new Location((unclaimedStadium / comms.EXTERNAL_PACK_FACTOR) % comms.EXTERNAL_PACK_FACTOR, unclaimedStadium % comms.EXTERNAL_PACK_FACTOR);
                    claimedObjectId = (unclaimedStadium / comms.EXTERNAL_PACK_FACTOR) / comms.EXTERNAL_PACK_FACTOR;
                    comms.claimStadiumAsPitcher(claimedObjectId, 1);
                }

                if (claimedObjectLocation == null) {
                    final int unclaimedBase = comms.nearestUnclaimedBaseAsPitcher();
                    if (unclaimedBase != -1) {
                        claimedObjectType = MapObject.BASE;
                        claimedObjectLocation = new Location((unclaimedBase / comms.EXTERNAL_PACK_FACTOR) % comms.EXTERNAL_PACK_FACTOR, unclaimedBase % comms.EXTERNAL_PACK_FACTOR);
                        claimedObjectId = (unclaimedBase / comms.EXTERNAL_PACK_FACTOR) / comms.EXTERNAL_PACK_FACTOR;
                        comms.claimBaseAsPitcher(claimedObjectId, 1);
                    }
                }
            }

            if (claimedObjectLocation != null) {
                if (claimedObjectType == MapObject.BASE) {
                    comms.claimBaseAsPitcher(claimedObjectId, 0);
                } else {
                    comms.claimStadiumAsPitcher(claimedObjectId, 0);
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
            } else if (uc.canMove()) {
                Util.tryMoveInOkayDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)], directionOkay);
            }

            endTurn();
        }
    }
}
