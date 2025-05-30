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

    @Override
    public boolean equals(Object obj){
        City other = (City) obj;
        
        if(this == obj){
            return true;
        }

        if(!(obj instanceof City)){
            return false;
        }

        return this.cityName.equals(other.cityName);
    }

    @Override
    public int hashCode(){
        return cityName.hashCode();
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
