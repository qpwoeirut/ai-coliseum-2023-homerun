package a_startplayer.util;


// based on https://github.com/TheK098/Battlecode2023/blob/main/src/qp1_13_tuning/utilities/FastRandom.java
// modified because static variables aren't allowed
public class FastRandom {
    private long x;
    public FastRandom(long x) {
        this.x = x;
    }

    public int nextInt() {
        x = (214013 * x + 2531011);
        return (int) (x >> 16) & 0x7FFF;
    }

    // [0, bound)
    public int nextInt(int bound) {  // technically the probability isn't uniform, but it shouldn't really matter
        final int ret = nextInt() % bound;
        return ret < 0 ? ret + bound : ret;
    }
}