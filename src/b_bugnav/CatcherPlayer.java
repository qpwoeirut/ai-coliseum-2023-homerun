package b_bugnav;

import aic2023.user.Direction;
import aic2023.user.Location;
import aic2023.user.UnitController;

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
            senseAndReportEnemies();

            if (uc.isOutOfMap(uc.getLocation().add(scoutDir.dx * 3, scoutDir.dy * 3)) || --scoutTimer == 0) {
                int shift = (int)(uc.getRandomDouble() * 6) + 1;
                if (shift >= 4) ++shift;
                scoutDir = Direction.values()[(scoutDir.ordinal() + shift) % 8];
                scoutTimer = SCOUT_TIMER;
            }
            Direction dir = bg.move(spawn.add(scoutDir.dx * 55, scoutDir.dy * 55));
            if (dir != null && dir != Direction.ZERO) {
                uc.move(dir);
            }

            uc.yield();
        }
    }
}
