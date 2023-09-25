package c2_squads.util;

import java.util.Arrays;

// from https://github.com/TheK098/Battlecode2023/blob/main/src/qp1_13_tuning/utilities/IntHashMap.java

/**
 * An int -> int hashmap
 * 0 is the default value for nonexistent keys
 */
public class IntHashMap {
    private final int n;
    //    private int size;
    private final int[] ind;
    private final int[][] keys, values;

    public IntHashMap(int bucketCount) {
        n = bucketCount;
        ind = new int[bucketCount];
        keys = new int[bucketCount][4];
        values = new int[bucketCount][4];
    }

    public void put(int key, int value) {
        int hash = key % n;
        int cind = ind[hash];
        int[] ckeys = keys[hash];
        int[] cvalues = values[hash];
        for (int i = cind; i --> 0;) {
            if (ckeys[i] == key) {
                cvalues[i] = value;
                return;
            }
        }
        if (cind == ckeys.length) {
            keys[hash] = Arrays.copyOf(ckeys, ckeys.length * 2);
            values[hash] = Arrays.copyOf(cvalues, ckeys.length * 2);
            keys[hash][cind] = key;
            values[hash][cind] = value;
        } else {
            ckeys[cind] = key;
            cvalues[cind] = value;
        }
        ++ind[hash];
//        size++;
    }

    public int get(int key) {
        int hash = key % n;
        int[] ckeys = keys[hash];
        int[] cvalues = values[hash];
        for (int i = ind[hash]; i --> 0;) {
            if (ckeys[i] == key) {
                return cvalues[i];
            }
        }
        return 0;
    }
}
