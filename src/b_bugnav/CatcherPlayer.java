package b_bugnav;

import aic2023.user.UnitController;

public class CatcherPlayer extends BasePlayer {
    CatcherPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        while (true) {
            comms.checkIn();
            senseAndReportBases();
            senseAndReportStadiums();
            senseAndReportEnemies();

            uc.move(bg.move_to_objects());
            uc.yield();
        }
    }
}
