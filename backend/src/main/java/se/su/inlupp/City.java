package se.su.inlupp;

public class City {

    private String cityName;
    private double x;
    private double y;

    public City(String cityName , double x , double y){
        this.cityName = cityName;
        this.x = x;
        this.y = y;
    }

    public String getCityName(){
        return cityName;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }
    
}
