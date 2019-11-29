package ru.kholstinin.testcb;

import java.util.Random;

public class City {
    private int id;
    private int x;
    private int y;

    public City(int id) {
        this.id = id;
        this.x = getRandomNumberInRange(0, 1000);
        this.y = getRandomNumberInRange(0, 1000);
        System.out.printf("Coordinate city id=%d, x=%d, y=%d\n", this.id, x, y);
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    private int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be higher Min");
        }
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}

