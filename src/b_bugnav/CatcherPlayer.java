package b_bugnav;

import aic2023.user.UnitController;

public class CatcherPlayer {
    private final UnitController uc;

    CatcherPlayer(UnitController uc) {
        this.uc = uc;
    }

    void run() {
        while (true) {
            uc.yield();
        }
    }
}
