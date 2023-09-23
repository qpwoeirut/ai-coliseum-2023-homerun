package b_bugnav;

import aic2023.user.UnitController;
import b_bugnav.util.FastRandom;

public class PitcherPlayer {
    private final UnitController uc;
    private final FastRandom random;

    PitcherPlayer(UnitController uc) {
        this.uc = uc;
        this.random = new FastRandom(uc.getInfo().getID());
    }

    void run() {
        while (true) {
            uc.yield();
        }
    }
}
