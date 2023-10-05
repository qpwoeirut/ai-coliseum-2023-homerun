package h4_queuescouting;

import aic2023.user.*;
import h4_queuescouting.util.Util;

public class CatcherPlayer extends BasePlayer {
    CatcherPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        int scoutTimer = 30;
        Location target = null;
        boolean reachedFocalPoint = false;
        while (true) {
            comms.checkIn();
            senseAndReportBases();
            senseAndReportStadiums();

            final UnitInfo[] nearbyEnemies = uc.senseUnits(REACHABLE_VISION, uc.getOpponent());
            final int directionOkay = calculateOkayDirections(nearbyEnemies);
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), nearbyEnemies, UnitType.BATTER);
//            debug("nearest enemy batter is " + nearestEnemyBatter);
            if (nearestEnemyBatter != null && ((directionOkay >> Direction.ZERO.ordinal()) & 1) > 0) {
                Util.tryMoveInDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()));
            }

            if (uc.canMove()) {
//                debugBytecode("start move");
                if (target == null) {
                    target = comms.popNearestScoutingQueue();
                    reachedFocalPoint = false;
                    if (target != null) scoutTimer = (int)(Util.movementDistance(uc.getLocation(), target) * 2);
                }
                while (target != null &&
                        (uc.getLocation().distanceSquared(target) <= 9 ||
                                (uc.getLocation().distanceSquared(target) <= VISION &&
                                        (uc.isOutOfMap(target) || uc.senseObjectAtLocation(target, false) == MapObject.WATER)))) {
//                    debugBytecode("scrapping target " + target);
                    target = comms.popNearestScoutingQueue();
                    reachedFocalPoint = false;
                    if (target != null) scoutTimer = (int)(Util.movementDistance(uc.getLocation(), target) * 2);
                    // update map boundaries?
                }
                if (target != null) {
//                    debug("target = " + target + ", scoutTimer = " + scoutTimer + ", lb dist = " + comms.lowerBoundDistance(uc.getLocation(), target));

                    Direction dir = null;
                    if (!reachedFocalPoint && comms.lowerBoundDistanceGreaterThan(uc.getLocation(), target, 6 * comms.DISTANCE_ROOT)) dir = comms.directionViaFocalPoint(target, directionOkay);
                    if (dir == Direction.ZERO) {
                        dir = null;
                        reachedFocalPoint = true;
                    }
                    if (dir == null) {
                        if (bg.target != target) bg.init(target);
                        dir = bg.bug0(target);  // don't bother with bug1. if bug0 fails, just change targets
                    }
                    if (dir == null) target = null;

                    if (dir != null && dir != Direction.ZERO && uc.canMove(dir) && ((directionOkay >> dir.ordinal()) & 1) > 0) {
                        uc.move(dir);
                    } else {
                        Util.tryMoveInOkayDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)], directionOkay);
                    }
                } else {
//                    debug("target = null");
                    Util.tryMoveInOkayDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)], directionOkay);
                }

                if (--scoutTimer <= 0) {
                    target = null;
                }
            }

            endTurn();
        }
    }
}
