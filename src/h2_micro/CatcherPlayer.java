package h2_micro;

import aic2023.user.*;
import h2_micro.util.Util;

public class CatcherPlayer extends BasePlayer {
    CatcherPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        int SCOUT_TIMER = 150;

        Location spawn = uc.getLocation();

        Direction scoutDir = Direction.values()[uc.getInfo().getID() % 8];
        int scoutTimer = SCOUT_TIMER;
        while (true) {
            comms.checkIn();
            senseAndReportBases();
            senseAndReportStadiums();

            final UnitInfo[] enemies = senseAndReportEnemies();
            final UnitInfo[] nearbyEnemies = uc.senseUnits(REACHABLE_VISION, uc.getOpponent());
            final int directionOkay = calculateOkayDirections(nearbyEnemies);
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), nearbyEnemies, UnitType.BATTER);
//            debug("nearest enemy batter is " + nearestEnemyBatter);
            if (nearestEnemyBatter != null && ((directionOkay >> Direction.ZERO.ordinal()) & 1) > 0) {
                Util.tryMoveInDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()));
            }

            if (uc.isOutOfMap(uc.getLocation().add(scoutDir.dx * 3, scoutDir.dy * 3)) || --scoutTimer == 0) {
                int shift = (int)(uc.getRandomDouble() * 6) + 1;
                if (shift >= 4) ++shift;
                scoutDir = Direction.values()[(scoutDir.ordinal() + shift) % 8];
                scoutTimer = SCOUT_TIMER;
            }
            final Direction dir = bg.move(spawn.add(scoutDir.dx * 55, scoutDir.dy * 55));
            if (dir != null && dir != Direction.ZERO) {
                Util.tryMoveInOkayDirection(uc, dir, directionOkay);
            } else {
                Util.tryMoveInOkayDirection(uc, scoutDir, directionOkay);
            }

            endTurn();
        }
    }
}
