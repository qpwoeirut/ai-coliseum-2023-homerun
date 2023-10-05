package h4_queuescouting;

import aic2023.user.*;
import h4_queuescouting.util.Util;

public class CatcherPlayer extends BasePlayer {
    CatcherPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final int SCOUT_TIMER = 100;
        int scoutTimer = SCOUT_TIMER;
        Location target = null;
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
                if (target == null) {
                    target = comms.popNearestScoutingQueue();
                }
                while (target != null && uc.getLocation().distanceSquared(target) <= VISION) {
                    target = comms.popNearestScoutingQueue();
                    // update map boundaries?
                }
                if (target != null) {
                    Direction dir = bg.move(target);
                    if (dir != null && dir != Direction.ZERO && uc.canMove(dir) && ((directionOkay >> dir.ordinal()) & 1) > 0) {
                        uc.move(dir);
                    } else {
                        Util.tryMoveInOkayDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)], directionOkay);
                    }
                } else {
                    Util.tryMoveInOkayDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)], directionOkay);
                }

                if (--scoutTimer <= 0) {
                    scoutTimer = SCOUT_TIMER;
                    target = null;
                }
            }

            endTurn();
        }
    }
}
