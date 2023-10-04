package h1_spiralscouting;

import aic2023.user.*;
import h1_spiralscouting.util.Util;

public class CatcherPlayer extends BasePlayer {
    CatcherPlayer(UnitController uc) {
        super(uc);
    }
    private final int HALF_MAP_SQUARED = 900;
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
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), enemies, UnitType.BATTER);
//            if (nearestEnemyBatter != null && Util.chebyshevDistance(uc.getLocation(), nearestEnemyBatter.getLocation()) <= 4) {
//                Util.tryMoveInDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()));
//            }
            if (uc.getInfo().getCurrentMovementCooldown() < 1) {
                Direction dir = sp.spiralMove(comms.getSelfHQLocation(), 0);
                if (dir == Direction.ZERO)
                {
                    dir = comms.directionViaFocalPoint(comms.getSelfHQLocation());
                }
                if (dir != null && dir != Direction.ZERO) {
                    uc.move(dir);
                }
            }

            endTurn();
        }
    }
}
