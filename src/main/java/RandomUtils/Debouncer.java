package RandomUtils;



// ik the names shit and all but i need the debouncer eslewhere and the messagehandler was
// greedy with it and had it all for himself and copying it in my new class felt cheap


public final class Debouncer {
    private static final long DEDUPE_WINDOW_MS = 500;
    private long last = 0;
    public boolean canFire() {
        long now = System.currentTimeMillis();
        if (now - last <= DEDUPE_WINDOW_MS) return false;
        last = now;
        return true;
    }
}