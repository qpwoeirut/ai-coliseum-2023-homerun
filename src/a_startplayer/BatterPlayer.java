package a_startplayer;

import aic2023.user.UnitController;
import a_startplayer.util.FastRandom;

public class BatterPlayer {
    private final UnitController uc;
    private final FastRandom random;

    BatterPlayer(UnitController uc) {
        this.uc = uc;
        this.random = new FastRandom(uc.getInfo().getID());
    }

    void run() {
        while (true) {
            uc.yield();
        }
    }
}
