package b_bugnav.util;

import aic2023.user.Location;
import aic2023.user.UnitController;
import aic2023.user.UnitType;

public class Communications {
    private final UnitController uc;

    private final int MAX_OBJECT_SIZE = 10000; // > 60^2 * 2
    private final int BASE_OFFSET = 0;
    private final int STADIUM_OFFSET = MAX_OBJECT_SIZE;
    private final int WATER_OFFSET = 2 * MAX_OBJECT_SIZE;

    private final int CHECK_IN_OFFSET = 100000;
    private final int BATTER_NUMBER = 0;
    private final int CATCHER_NUMBER = 1;
    private final int PITCHER_NUMBER = 2;
    private final int HQ_NUMBER = 3;

    public Communications(UnitController uc) {
        this.uc = uc;
    }

    public void reportNewBases(Location[] bases) {
        reportNewObjects(bases, BASE_OFFSET);
    }

    public void reportNewStadiums(Location[] stadiums) {
        reportNewObjects(stadiums, STADIUM_OFFSET);
    }

    public void reportNewWater(Location[] water) {
        reportNewObjects(water, WATER_OFFSET);
    }

    private void reportNewObjects(Location[] locs, int offset) {
        final int n = uc.read(offset);
        int m = 0;
        for (int locIdx = locs.length - 1; locIdx >= 0; --locIdx) {
            boolean alreadyExists = false;
            for (int i = n; i > 0 && !alreadyExists; --i) {
                alreadyExists = uc.read(offset + 2 * i) == locs[locIdx].x && uc.read(offset + 2 * i + 1) == locs[locIdx].y;
            }
            if (!alreadyExists) {
                ++m;
                uc.write(offset + 2 * (n + m), locs[locIdx].x);
                uc.write(offset + 2 * (n + m) + 1, locs[locIdx].y);
            }
        }
        uc.write(offset, n + m);
    }

    private void reportNewObject(Location loc, int offset) {
        int n = uc.read(offset);
        for (int i = n; i > 0; --i) {
            if (uc.read(offset + 2 * i) == loc.x && uc.read(offset + 2 * i + 1) == loc.y) return;
        }

        ++n;
        uc.write(offset, n);
        uc.write(offset + 2 * n, loc.x);
        uc.write(offset + 2 * n + 1, loc.y);
    }

    public void checkIn() {
        final int typeNumber = uc.getType() == UnitType.BATTER ? BATTER_NUMBER : (uc.getType() == UnitType.CATCHER ? CATCHER_NUMBER : (uc.getType() == UnitType.PITCHER ? PITCHER_NUMBER : HQ_NUMBER));
        final int currentIndex = CHECK_IN_OFFSET + uc.getRound() * 4 + typeNumber;
        final int lastIndex = currentIndex - 4;
        final int lastRoundCount = uc.read(lastIndex);
        uc.write(lastIndex, lastRoundCount - 1);

        final int currentRoundCount = uc.read(currentIndex);
        uc.write(currentIndex, currentRoundCount + 1);
    }

    public int countBases() {
        return uc.read(BASE_OFFSET);
    }
    public int countStadiums() {
        return uc.read(STADIUM_OFFSET);
    }
    public int countBatters() {
        return countUnits(BATTER_NUMBER);
    }
    public int countCatchers() {
        return countUnits(CATCHER_NUMBER);
    }
    public int countPitchers() {
        return countUnits(PITCHER_NUMBER);
    }
    private int countUnits(int typeNumber) {
        final int currentIndex = CHECK_IN_OFFSET + uc.getRound() * 4 + typeNumber;
        final int lastIndex = currentIndex - 4;
        return uc.read(lastIndex) + uc.read(currentIndex);
    }
}
