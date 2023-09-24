package b_bugnav;

import aic2023.user.MapObject;
import aic2023.user.UnitController;

public class CatcherPlayer {
    private final UnitController uc;
    private final BugMover bg;
    CatcherPlayer(UnitController uc) {
        this.uc = uc;
        this.bg = new BugMover(uc);
    }

    void run() {
        while (true) {
            uc.move(bg.move_to_objects());
            uc.yield();
        }
    }
}
