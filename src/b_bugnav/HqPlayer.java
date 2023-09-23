package b_bugnav;

import aic2023.user.Direction;
import aic2023.user.UnitController;
import aic2023.user.UnitType;
import b_bugnav.util.FastRandom;

public class HqPlayer {
    private final UnitController uc;
    private final FastRandom random;

    HqPlayer(UnitController uc) {
        this.uc = uc;
        this.random = new FastRandom(uc.getInfo().getID());
    }

    void run() {
        while (true) {
            Direction dir = Direction.values()[random.nextInt(8)];
            if (uc.canRecruitUnit(UnitType.PITCHER, dir)) uc.recruitUnit(UnitType.PITCHER, dir);
            uc.yield();
        }
    }
}
