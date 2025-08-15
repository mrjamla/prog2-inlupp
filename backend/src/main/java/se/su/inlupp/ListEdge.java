package se.su.inlupp;

import java.util.Objects;

public class ListEdge<T> implements Edge<T>{
    private final T destination;
    private final String name;
    private int weight;

    public ListEdge(T destination, String name, int weight){
        this.destination = destination;
        this.name = name;
        if(weight < 0) {
            throw new IllegalArgumentException(); // unchecked exception, negativ vikt
        } else{
            this.weight = weight;
        }
    }

    @Override
    public int getWeight(){
        return weight;
    }

    @Override
    public void setWeight(int weight){
        Objects.requireNonNull(weight);
        if (weight < 0) {
            throw new IllegalArgumentException();
        } else {
            this.weight = weight;
        }
    }

    @Override
    public T getDestination() {
        return this.destination;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString(){
        return String.format("to %s by %s takes %d", destination, name, weight);
        // return "till " + destination + " med " + name + " tar " + weight;
    }
}
