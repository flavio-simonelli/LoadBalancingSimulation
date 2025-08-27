package it.pmcsn.lbsim.models.domain.server;

import java.util.PriorityQueue;
import java.util.Queue;

public class ServerIdAllocator {
    private int nextId = 1;  // prossimo id se non ci sono slot liberi
    private final Queue<Integer> freeIds = new PriorityQueue<>();

    /**
     * Restituisce un nuovo ID server.
     * Se ci sono ID liberi, prende il più piccolo disponibile.
     */
    public int allocate() {
        if (!freeIds.isEmpty()) {
            return freeIds.poll();
        }
        return nextId++;
    }

    /**
     * Libera un ID server (che potrà essere riutilizzato).
     */
    public void release(int id) {
        if (id > 0) {
            freeIds.add(id);
        }
    }
}
