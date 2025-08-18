package it.pmcsn.lbsim.models;

import java.util.LinkedList;
import java.util.Queue;

public class SlidingWindowResponseTime {
    private Queue<Double> queue;    // List to store response times
    private final int windowSize;   // Size of the sliding window

    public SlidingWindowResponseTime(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be greater than 0");
        }
        this.windowSize = windowSize;
        this.queue = new LinkedList<>(); // Initialize the queue
    }

    public void add(double rt) {
        if (queue.size() == windowSize) {
            queue.poll(); // remove the first element
        }
        queue.add(rt); // add the new response time into the queue
    }

    public double getAverage() {
        if (queue.isEmpty()) {
            return 0.0; // Return 0 if the queue is empty
        }
        double sum = 0.0;
        for (double rt : queue) {
            sum += rt; // Sum all response times in the queue
        }
        return sum / queue.size(); // Calculate and return the average
    }



}
