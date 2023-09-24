package b_bugnav;

import aic2023.user.UnitController;
import b_bugnav.util.Communications;

public class CatcherPlayer {
    private final UnitController uc;
    private final Communications comms;
    private final BugMover bg;
    CatcherPlayer(UnitController uc) {
        this.uc = uc;
        this.comms = new Communications(uc);
        this.bg = new BugMover(uc);
    }

    void run() {
        while (true) {
            comms.checkIn();
            uc.move(bg.move_to_objects());
            uc.yield();
        }
    }
}
