package ru.kholstinin.testcb;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;

public class TSPSolver {

    final Object lock = new Object();
    private long distance[][];
    private List<? extends City> cities; // First element in labels must be home position (0,0)
    private List<Integer> bestPath = null;
    private long bestPathLength = Integer.MAX_VALUE;
    private long startMillies, bestSolution = 0;
    private boolean bestSolutionFound = false;
    private int costX;
    private boolean abort;
    private int costY;
    private AtomicLong tasks;
    private AtomicLong done;

    public TSPSolver(List<? extends City> labels) {
        this(labels, 1, 1);
    }

    /**
     * @param cities - point list
     * @param costX  - the path cost for axle X. f.e. max axle X speed
     * @param costY  - the path cost for axle Y. f.e. max axle Y speed
     */
    public TSPSolver(List<? extends City> cities, int costX, int costY) {
        done = new AtomicLong();
        tasks = new AtomicLong();
        this.costX = costX;
        this.costY = costY;
        this.cities = cities;
        abort = false;
    }

    public long getAllTasks() {
        return tasks.get();
    }

    public long getDoneTasks() {
        return done.get();
    }

    public void compute() {
        // Calculate distance matrix for all points
        distance = new long[cities.size()][cities.size()];
        for (int i = -1; ++i < cities.size(); ) {
            int x1 = cities.get(i).getX();
            int y1 = cities.get(i).getY();
            distance[i][i] = 0;
            for (int j = -1; ++j < i; ) {
                int x2 = cities.get(j).getX();
                int y2 = cities.get(j).getY();
                distance[i][j] = Math.max(Math.abs(x1 - x2) * costX, Math.abs(y1 - y2) * costY);
                distance[j][i] = distance[i][j];
            }
        }
        ArrayList<Integer> freeLabels = new ArrayList<>(cities.size());
        startMillies = System.currentTimeMillis();
        for (int i = -1; ++i < cities.size(); ) {
            freeLabels.add(i);
        }
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new TSPAction(new ArrayList<Integer>(), freeLabels, 0, 0, null));
        if (!abort) {
            bestSolutionFound = true;
        }
        System.out.println("Execution time is " + (System.currentTimeMillis() - startMillies) + " ms");
    }

    public String printPath(List<Integer> currentPath) {
        StringBuilder path = new StringBuilder();
        path.append("{ ");
        for (int i = -1; ++i < currentPath.size(); ) {
            if (i == 0) {
                path.append(" ");
                path.append(cities.get(currentPath.get(i)));
            } else {
                path.append(" | ");
                path.append(cities.get(currentPath.get(i)));
            }
        }
        path.append(" }");
        return path.toString();
    }

    private void removePoint(List<Integer> array, int value) {
        for (int i = -1; ++i < array.size(); ) {
            if (array.get(i) == value) {
                array.remove(i);
                return;
            }
        }
    }

    /**
     * Find the nearest not used point in distance array for point with curPos index
     */
    private int getNextNearestPoint(int curPos, ArrayList<Integer> freePoints) {
        long minDistance = Long.MAX_VALUE;
        int minIndex = -1;
        for (Integer freePoint : freePoints) {
            // find not used nearest point
            if (minDistance > distance[curPos][freePoint]) {
                minDistance = distance[curPos][freePoint];
                minIndex = freePoint;
            }
        }
        removePoint(freePoints, minIndex);
        return minIndex;
    }

    public List<Integer> getBestPath() {
        while (bestPath == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        abort = true;
        synchronized (lock) {
            return bestPath;
        }
    }

    public long getBestPathLength() {
        return bestPathLength;
    }

    private void savePath(ArrayList<Integer> curPath, long curLength) {
        if (curLength < bestPathLength) {
            synchronized (lock) {
                if (curLength < bestPathLength) {
                    bestPathLength = curLength;
                    bestSolution = System.currentTimeMillis();
                    bestPath = (ArrayList<Integer>) curPath.clone();
                }
            }
            System.out.println(tasks.get() + " shots. Path found in " + (bestSolution - startMillies) + " ms. Length=" + curLength); // + " > " + printPath(curPath));
        }
    }

    public boolean isBestSolutionFound() {
        return bestSolutionFound;
    }

    public void setAbort() {
        abort = true;
    }

    class TSPAction extends RecursiveAction {

        private final ArrayList<Integer> currentPath;
        private final ArrayList<Integer> freePoints;
        private final int curPos;
        private final long length;
        private final TSPAction next;

        public TSPAction(ArrayList<Integer> currentPath, ArrayList<Integer> freePoints, int curPos, long length, TSPAction next) {
            this.currentPath = currentPath;
            this.freePoints = freePoints;
            this.curPos = curPos;
            this.length = length;
            this.next = next;
            tasks.incrementAndGet();
        }

        @Override
        protected void compute() {
            int nextIndex;
            TSPAction nextLevel = null;
            if (bestPathLength < length || abort) {
                // is current length > best length => go back
                done.incrementAndGet();
                return;
            }
            ArrayList<Integer> newPath = (ArrayList) currentPath.clone();
            newPath.add(curPos);
            ArrayList<Integer> newFreePoints = (ArrayList) freePoints.clone();
            removePoint(newFreePoints, curPos);

            if (newFreePoints.isEmpty()) {
                // no more unchecked points (max recursion level reached)
                savePath(newPath, length);
            } else {
                ArrayList<Integer> localFreePoints = (ArrayList) newFreePoints.clone();

                // at first compute the shortest path
                nextIndex = getNextNearestPoint(curPos, localFreePoints);
                if (nextIndex >= 0) {
                    new TSPAction(newPath, newFreePoints, nextIndex, length + distance[curPos][nextIndex], nextLevel).compute();
                }

                // then add all other points to JoinFork pool
                while (localFreePoints.size() > 0 && !abort) {
                    nextIndex = getNextNearestPoint(curPos, localFreePoints);
                    if (nextIndex >= 0) {
                        nextLevel = new TSPAction(newPath, newFreePoints, nextIndex, length + distance[curPos][nextIndex], nextLevel);
                        nextLevel.fork();
                    }
                }
                // now try to unfork a task and help with computation or wait for task end
                while (nextLevel != null) {
                    if (nextLevel.tryUnfork()) {
                        nextLevel.compute();
                    } else {
                        nextLevel.join();
                    }
                    nextLevel = nextLevel.getNext();
                }
            }
            done.incrementAndGet();
        }

        public TSPAction getNext() {
            return next;
        }

    }
}