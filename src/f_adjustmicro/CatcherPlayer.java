package f_adjustmicro;

import aic2023.user.*;
import f_adjustmicro.util.Util;

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
            senseAndReportGrassIfNecessary();

            final UnitInfo[] enemies = senseAndReportEnemies();
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), enemies, UnitType.BATTER);
            if (nearestEnemyBatter != null && Util.chebyshevDistance(uc.getLocation(), nearestEnemyBatter.getLocation()) <= 4) {
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
                uc.move(dir);
            }

            comms.useRemainingBytecode();
            uc.yield();
        }
    }
}
