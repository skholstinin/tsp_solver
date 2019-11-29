package ru.kholstinin.testcb;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class main {

    private static int getDistanceBetweenCity(City city1, City city2) {
        return (int) Math.sqrt(Math.abs(city1.getX() - city2.getX()) * Math.abs(city1.getX() - city2.getX())
                + Math.abs(city1.getY() - city2.getY()) * Math.abs(city1.getY() - city2.getY()));
    }

    public static void main(String[] args) {
        int[][] distance;
        List<City> cityList = new ArrayList<City>();
        System.out.println("Start solver");
        Scanner in = new Scanner(System.in);
        System.out.println("Введите кол-во городов");
        int num = in.nextInt();
        distance = new int[num][num];
        System.out.printf("Вы выбрали %d городов\n", num);
        System.out.println("Начинаем строить кротчайший путь");
        for (int i = 0; i < num; i++) {
            cityList.add(new City(i));
        }
        for (int k = 0; k < cityList.size(); k++) {
            for (int j = 0; j < cityList.size(); j++) {
                distance[k][j] = getDistanceBetweenCity(cityList.get(k), cityList.get(j));
                System.out.printf("distance=%d between city id=%d and city id=%d\n", distance[k][j], cityList.get(k).getId(), cityList.get(j).getId());
            }
        }
    }
}

