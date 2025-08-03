package se.su.inlupp;

public class Place{

    private String name;
    private double x;
    private double y;

    public Place(String name , double x , double y){
        this.name = name;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj){
        Place other = (Place) obj;
        
        if(this == obj){
            return true;
        }

        if(!(obj instanceof Place)){
            return false;
        }
        // två platser är lika om de delar namn (case-insensitive)
        return this.name.toLowerCase().equals(other.name.toLowerCase()); //&& this.x == other.x && this.y == other.y;
    }

    @Override
    public int hashCode(){
        return name.hashCode();
    }

    public String getName(){
        return name;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }
    
    @Override
    public String toString(){
        return "%s (%f, %f)".formatted(name, x, y);
    }
}
