package h3_linescouting;

import aic2023.user.*;
import h3_linescouting.util.Util;

public class CatcherPlayer extends BasePlayer {
    CatcherPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final int SCOUT_TIMER = 50;

        final Location SPAWN = uc.getLocation();
        Direction scoutDir = comms.getSelfHQLocation().directionTo(SPAWN);
        int scoutTimer = SCOUT_TIMER;
        int distance = 20;
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

            if (uc.isOutOfMap(uc.getLocation().add(scoutDir.dx * 4, scoutDir.dy * 4)) || --scoutTimer == 0) {
                scoutDir = Direction.values()[(scoutDir.ordinal() + 2) % 8];
                scoutTimer = SCOUT_TIMER;
                distance += 5;
            }
            final Direction dir = bg.move(SPAWN.add(scoutDir.dx * distance, scoutDir.dy * distance));
            if (dir != null && dir != Direction.ZERO) {
                Util.tryMoveInOkayDirection(uc, dir, directionOkay);
            } else {
                Util.tryMoveInOkayDirection(uc, scoutDir, directionOkay);
            }

            endTurn();
        }
    }
}
