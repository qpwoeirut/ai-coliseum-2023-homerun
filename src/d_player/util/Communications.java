package d_player.util;

import aic2023.user.Location;
import aic2023.user.UnitController;
import aic2023.user.UnitType;

public class Communications {
    private final UnitController uc;

    // TODO: for final code submissions, if we're tight on bytecode, we can inline the constants

    // for objects (bases, stadiums, water)
    // a[0] = number of specified object
    // a[4i + 1 ... 4i + 4] describes object i with (x, y, id of claiming pitcher or 0 if unclaimed, round that claim was most recently updated)
    private final int OBJECT_SIZE = 4;
    private final int OBJECT_X = 1;
    private final int OBJECT_Y = 2;
    private final int CLAIM_ID = 3;
    private final int CLAIM_ROUND = 4;
    private final int NO_CLAIM = 0;  // IDs are in [1, 10000]
    private final int CLAIM_EXPIRATION = 3;  // if a pitcher doesn't update their claim in 3 rounds, assume they've died

    private final int MAX_OBJECT_COUNT = 4000; // > 60^2
    private final int BASE_OFFSET = 0;
    private final int STADIUM_OFFSET = MAX_OBJECT_COUNT * OBJECT_SIZE;
    private final int WATER_OFFSET = 2 * MAX_OBJECT_COUNT * OBJECT_SIZE;

    private final int CHECK_IN_OFFSET = 100000;
    private final int BATTER_NUMBER = 0;
    private final int CATCHER_NUMBER = 1;
    private final int PITCHER_NUMBER = 2;
    private final int HQ_NUMBER = 3;

    private final int ENEMY_SIGHTING_OFFSET = 200000;
    private final int ENEMY_SIZE = 3;
    private final int ENEMY_X = 1;
    private final int ENEMY_Y = 2;
    private final int ENEMY_URGENCY = 3;
    private final int ENEMY_MERGE_DISTANCE = 30;

    private final int UNIT_OFFSET = 300000;
     private final int UNIT_SIZE = 1;  // change this to # of properties
    // enumerate property names here
    // PROPERTY_NAME = 1
    // PROPERTY_NAME = 2
    // PROPERTY_NAME = 3

    // for functions that require arrays to be returned. this is more efficient because we don't need to allocate an array each time
    public Location[] returnedLocations = new Location[20];  // for both map objects and enemy sightings
    public int[] returnedIds = new int[8];  // only used for map objects
    public int[] returnedUrgencies = new int[20];  // only used for enemy sightings

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
            for (int i = n - 1; i >= 0 && !alreadyExists; --i) {
                alreadyExists = uc.read(offset + OBJECT_SIZE * i + OBJECT_X) == locs[locIdx].x && uc.read(offset + OBJECT_SIZE * i + OBJECT_Y) == locs[locIdx].y;
            }
            if (!alreadyExists) {
                uc.write(offset + OBJECT_SIZE * (n + m) + OBJECT_X, locs[locIdx].x);
                uc.write(offset + OBJECT_SIZE * (n + m) + OBJECT_Y, locs[locIdx].y);
                uc.write(offset + OBJECT_SIZE * (n + m) + CLAIM_ID, NO_CLAIM);
                uc.write(offset + OBJECT_SIZE * (n + m) + CLAIM_ROUND, NO_CLAIM);
                ++m;
            }
        }
        uc.write(offset, n + m);
    }

    private void reportNewObject(Location loc, int offset) {
        int n = uc.read(offset);
        for (int i = n; i > 0; --i) {
            if (uc.read(offset + OBJECT_SIZE * i + 1) == loc.x && uc.read(offset + OBJECT_SIZE * i + 2) == loc.y) return;
        }

        ++n;
        uc.write(offset, n);
        uc.write(offset + OBJECT_SIZE * n + OBJECT_X, loc.x);
        uc.write(offset + OBJECT_SIZE * n + OBJECT_Y, loc.y);
        uc.write(offset + OBJECT_SIZE * n + CLAIM_ID, NO_CLAIM);
        uc.write(offset + OBJECT_SIZE * n + CLAIM_ROUND, NO_CLAIM);
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

    /**
     * Lists all bases that have not yet been claimed.
     * @return int: the number of unclaimed bases
     * The base locations can be accessed in the public returnedLocations array.
     * The IDs (which are internal IDs used by the Communications class) can be accessed in the public returnedIds array.
     */
    public int listUnclaimedBases() {
        return listUnclaimedObjects(BASE_OFFSET);
    }

    /**
     * Lists all stadiums that have not yet been claimed.
     * @return int: the number of unclaimed stadiums
     * The base locations can be accessed in the public returnedLocations array
     * The IDs (which are internal IDs used by the Communications class) can be accessed in the public returnedIds array.
     */
    public int listUnclaimedStadiums() {
        return listUnclaimedObjects(STADIUM_OFFSET);
    }

    private int listUnclaimedObjects(int offset) {
        final int totalObjects = uc.read(offset);
        if (returnedLocations.length < totalObjects || returnedIds.length < totalObjects) {
            returnedLocations = new Location[totalObjects];
            returnedIds = new int[totalObjects];
        }

        int n = 0;
        for (int i = totalObjects - 1; i >= 0; --i) {
            if (readObjectProperty(offset, i, CLAIM_ID) == NO_CLAIM || readObjectProperty(offset, i, CLAIM_ROUND) + CLAIM_EXPIRATION < uc.getRound()) {
                returnedLocations[n] = new Location(readObjectProperty(offset, i, OBJECT_X), readObjectProperty(offset, i, OBJECT_Y));
                returnedIds[n++] = i;
            }
        }
        return n;
    }

    public void claimBase(int baseId) {
        uc.write(BASE_OFFSET + baseId * OBJECT_SIZE + CLAIM_ID, uc.getInfo().getID());
        updateClaimOnBase(baseId);
    }
    public void claimStadium(int stadiumId) {
        uc.write(STADIUM_OFFSET + stadiumId * OBJECT_SIZE + CLAIM_ID, uc.getInfo().getID());
        updateClaimOnStadium(stadiumId);
    }
    public void updateClaimOnBase(int baseId) {
        uc.write(BASE_OFFSET + baseId * OBJECT_SIZE + CLAIM_ROUND, uc.getRound());
    }
    public void updateClaimOnStadium(int stadiumId) {
        uc.write(STADIUM_OFFSET + stadiumId * OBJECT_SIZE + CLAIM_ROUND, uc.getRound());
    }

    private int readObjectProperty(int offset, int index, int property) {
        return uc.read(offset + OBJECT_SIZE * index + property);
    }

    /**
     * Records an enemy sighting. Will try to combine this sighting with an existing sighting, if a nearby one exists.
     * Prioritizing combining first will ensure that we don't end up with too many items in the array.
     * If no nearby sighting exists, will try to replace an old sighting. Otherwise will add to end of the array.
     * @param loc     location where enemy was spotted
     * @param urgency how urgently help is required (higher -> scarier)
     */
    public void reportEnemySighting(Location loc, int urgency) {
        final int currentSightingCount = uc.read(ENEMY_SIGHTING_OFFSET);
        for (int i = currentSightingCount - 1; i >= 0; --i) {
            if (loc.distanceSquared(new Location(readSightingProperty(i, ENEMY_X), readSightingProperty(i, ENEMY_Y))) <= ENEMY_MERGE_DISTANCE) {
                final int oldUrgency = readSightingProperty(i, ENEMY_URGENCY);
                // sightings are close enough that we can merge them
                writeSightingProperty(i, ENEMY_URGENCY, (int)(Math.cbrt(oldUrgency * oldUrgency * oldUrgency + urgency * urgency * urgency) + 0.999));  // TODO: is cbrt bytecode-expensive?
                return;
            }
        }
        for (int i = currentSightingCount - 1; i >= 0; --i) {
            if (readSightingProperty(i, ENEMY_URGENCY) <= 0) {  // sighting has decayed, we can replace it
                writeSightingProperty(i, ENEMY_X, loc.x);
                writeSightingProperty(i, ENEMY_Y, loc.y);
                writeSightingProperty(i, ENEMY_URGENCY, urgency);
                return;
            }
        }

//        uc.println("adding sighting on round " + uc.getRound() + " at " + loc + ". urgency " + urgency);
        writeSightingProperty(currentSightingCount, ENEMY_X, loc.x);
        writeSightingProperty(currentSightingCount, ENEMY_Y, loc.y);
        writeSightingProperty(currentSightingCount, ENEMY_URGENCY, urgency);
        uc.write(ENEMY_SIGHTING_OFFSET, currentSightingCount + 1);
    }

    /**
     * Lists all enemy sighting locations and urgencies.
     * Urgencies take into account the distance between the unit and the enemy sighting
     * @return int: the number of enemy sightings
     * The enemy locations can be accessed in the public returnedLocations array.
     * The urgency of each sighting can be accessed in the public returnedUrgency array.
     * No IDs are returned. The returnedIds array is not modified and will likely contain stale data.
     */
    public int listEnemySightings() {
        final int totalEnemySightings = uc.read(ENEMY_SIGHTING_OFFSET);
        if (returnedLocations.length < totalEnemySightings || returnedUrgencies.length < totalEnemySightings) {
            returnedLocations = new Location[totalEnemySightings];
            returnedUrgencies = new int[totalEnemySightings];
        }
        int n = 0;
        for (int i = totalEnemySightings - 1; i >= 0; --i) {
            if (readSightingProperty(i, ENEMY_URGENCY) > 0) {
                returnedLocations[n] = new Location(readSightingProperty(i, ENEMY_X), readSightingProperty(i, ENEMY_Y));
                returnedUrgencies[n] = readSightingProperty(i, ENEMY_URGENCY) * 10 - uc.getLocation().distanceSquared(returnedLocations[n]);
                ++n;
            }
        }
        return n;
    }

    // called once per round by the HQ
    public void decayEnemySightingUrgencies() {
        int hi = uc.read(ENEMY_SIGHTING_OFFSET) - 1;
        for (int lo = 0; lo <= hi; ++lo) {
            while (lo <= hi && readSightingProperty(hi, ENEMY_URGENCY) <= 0) --hi;
            if (lo < hi && readSightingProperty(lo, ENEMY_URGENCY) <= 0) {
                writeSightingProperty(lo, ENEMY_X, readSightingProperty(hi, ENEMY_X));
                writeSightingProperty(lo, ENEMY_Y, readSightingProperty(hi, ENEMY_Y));
                writeSightingProperty(lo, ENEMY_URGENCY, readSightingProperty(hi, ENEMY_URGENCY) - 1);
                --hi;
            } else if (lo <= hi) {
                writeSightingProperty(lo, ENEMY_URGENCY, readSightingProperty(lo, ENEMY_URGENCY) - 1);
            }
        }
        uc.write(ENEMY_SIGHTING_OFFSET, hi + 1);
    }

    private int readSightingProperty(int index, int property) {
        return uc.read(ENEMY_SIGHTING_OFFSET + ENEMY_SIZE * index + property);
    }
    private void writeSightingProperty(int index, int property, int value) {
        uc.write(ENEMY_SIGHTING_OFFSET + ENEMY_SIZE * index + property, value);
    }

    // write public methods to read/write communications here
    private int readUnitProperty(int unitId, int property) {
        return uc.read(UNIT_OFFSET + UNIT_SIZE * unitId + property);
    }
    private void writeUnitProperty(int unitId, int property, int value) {
        uc.write(UNIT_OFFSET + UNIT_SIZE * unitId + property, value);
    }
}
