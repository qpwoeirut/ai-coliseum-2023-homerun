package b_bugnav;

import aic2023.user.UnitController;
import b_bugnav.util.FastRandom;

public class CatcherPlayer {
    private final UnitController uc;
    private final FastRandom random;

    CatcherPlayer(UnitController uc) {
        this.uc = uc;
        this.random = new FastRandom(uc.getInfo().getID());
    }

    void run() {
        while (true) {
            uc.yield();
        }
    }
}
