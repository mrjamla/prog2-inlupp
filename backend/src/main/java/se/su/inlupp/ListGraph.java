package se.su.inlupp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class ListGraph<T> implements Graph<T> {

  private final Map<T, Set<Edge<T>>> nodes = new HashMap<>();

  @Override
  public void add(T node) {
    Objects.requireNonNull(node, "Null är ingen nod!");
    nodes.putIfAbsent(node, new HashSet<>());
  }

  @Override
  public void connect(T node1, T node2, String name, int weight) throws NoSuchElementException {
    if (nodes.containsKey(node1) && nodes.containsKey(node2)) {
      // OBS: kontrollerar om kant finns från node1 till node2, bör räcka så länge det
      // bara går att lägga till oriktade förbindelser
      Collection<Edge<T>> edges1 = getEdgesFrom(node1);
      for (Edge<T> edge : edges1) {
        if (edge.getDestination().equals(node2)) {
          throw new IllegalStateException(); // unchecked exception, kant finns redan
        }
      }
      nodes.get(node1).add(new ListEdge<>(node2, name, weight));
      nodes.get(node2).add(new ListEdge<>(node1, name, weight));
    } else {
      throw new NoSuchElementException(); // checked exception, någon nod saknas i grafen
    }
  }

  @Override
  public void setConnectionWeight(T node1, T node2, int weight) throws NoSuchElementException {
    getEdgeBetween(node1, node2).setWeight(weight);
    getEdgeBetween(node2, node1).setWeight(weight);
  }

  @Override
  public Set<T> getNodes() {
    return new HashSet<>(nodes.keySet());
  }

  @Override
  public Collection<Edge<T>> getEdgesFrom(T node) throws NoSuchElementException {
    if (nodes.containsKey(node)) {
      return new HashSet<>(nodes.get(node));
    } else {
      throw new NoSuchElementException(); // checked exception, noden saknas i grafen
    }
  }

  @Override
  public Edge<T> getEdgeBetween(T node1, T node2) throws NoSuchElementException {
    if (nodes.containsKey(node2)) {
      Collection<Edge<T>> edges1 = getEdgesFrom(node1);
      for (Edge<T> edge : edges1) {
        if (edge.getDestination().equals(node2)) {
          return edge;
        }
      }
    } else {
      throw new NoSuchElementException(); //  andra noden saknas i grafen
    }
    return null; // kant saknas mellan noder
  }

  @Override
  public void disconnect(T node1, T node2) throws NoSuchElementException {
    Edge<T> edgeTo2 = getEdgeBetween(node1, node2);
    Edge<T> edgeTo1 = getEdgeBetween(node2, node1);
    if (edgeTo2 == null || edgeTo1 == null) {
      throw new IllegalStateException(); // unchecked exception, kant saknas mellan noder
    } else {
      nodes.get(node1).remove(edgeTo2);
      nodes.get(node2).remove(edgeTo1);
    }
  }

  @Override
  public void remove(T node) throws NoSuchElementException {
    Collection<Edge<T>> edges = getEdgesFrom(node);
    for (Edge<T> edge : edges) {
      T destination = edge.getDestination();
      disconnect(node, destination);
    }
    nodes.remove(node);
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder("Graph");
    stringBuilder.append("\n");
    for (Map.Entry<T, Set<Edge<T>>> mapEntry : nodes.entrySet()) {
      stringBuilder.append(mapEntry.getKey()).append(": ").append(mapEntry.getValue()).append("\n");
    }
    return stringBuilder.toString();
  }

  @Override
  public boolean pathExists(T from, T to) {
    if (nodes.containsKey(from) && nodes.containsKey(to)) {
      Set<T> visited = new HashSet<>(); // vill inte besöka redan besökta noder
      return isAPath(from, to, visited); // följ kanter från from tills vi når to eller tills alla vägar från from genomsökts
    }
    return false;
  }

  private boolean isAPath(T from, T to, Set<T> visited) {
    visited.add(from); // markera noden som besökt
    if (from.equals(to)) { // Är noden den vi söker?
      return true;
    }
    // för varje granne som är ansluten via en kant till noden
    for (Edge<T> edge : getEdgesFrom(from)) {
      // Har vi inte redan besökt grannen tidigare?
      if (!visited.contains(edge.getDestination())) {
        // rekursivt anrop, besök granne och se om vägen via denna leder till sökta
        // noden
        if (isAPath(edge.getDestination(), to, visited)) {
          return true;
        }
      }
      // vägen ledde inte till den sökta noden, hitta nästa kant till en obesökt
      // granne
    }
    // ingen av vägar via den här nodens grannar leder till sökta noden
    return false;
  }

  @Override
  public List<Edge<T>> getPath(T from, T to) {
    Map<T, T> connection = new HashMap<>();
    recursiveConnect(from, null, connection); // följ kanter från startnoden och spara en koppling för varje nod längst vägen som går att nå från den                                             // startnoden (from)
    LinkedList<Edge<T>> path = new LinkedList<>();
    T current = to; // börja från slutnoden som vald nod
    while (current != null && !current.equals(from)) { // så länge vald nod inte är null OCH vi inte har nåt startnoden
      T next = connection.get(current); // hämta nästa nod på vägen till start, om den finns, annars null
      if (next != null) { // finns ingen koppling till en annan nod, så hoppa över att hämta kant (null, current) och undvik felmeddelande 
        Edge<T> edge = getEdgeBetween(next, current); // hämta kant mellan nästa nod, ett steg närmare start, och vald nod
        path.addFirst(edge); // lägg kanter på varandra ("stacka dem") så de kommer i rätt ordning
      }
      current = next; // välj nästa nod som vald nod och upprepa
    }
    return current == null ? null : path; // om vi når ett null istället för from-noden, så returnas att väg saknas (null) annars en väg (path) 
  }

  private void recursiveConnect(T to, T from, Map<T, T> connection) {
    connection.put(to, from); // lägg in att vi besökt to-noden från from-noden (en koppling)
    for (Edge<T> edge : getEdgesFrom(to)) { // sök igenom kanter kopplade till to-nod
      if (!connection.containsKey(edge.getDestination())) { // om vi ännu inte besök destinationsnoden i en kant
        recursiveConnect(edge.getDestination(), to, connection); // besök destinationen, lägg in hur vi kom till den (från to-noden) och leta efter obesökta noder via den nya nodens kanter
      }
      // annars leta vidare bland andra kanter efter obesökta noder
    }
  }
}
