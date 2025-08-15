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
          throw new IllegalStateException();
        }
      }
      nodes.get(node1).add(new ListEdge<>(node2, name, weight));
      nodes.get(node2).add(new ListEdge<>(node1, name, weight));
    } else {
      throw new NoSuchElementException();
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
      throw new NoSuchElementException();
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
      throw new NoSuchElementException();
    }
    return null;
  }

  @Override
  public void disconnect(T node1, T node2) throws NoSuchElementException {
    Edge<T> edgeTo2 = getEdgeBetween(node1, node2);
    Edge<T> edgeTo1 = getEdgeBetween(node2, node1);
    if (edgeTo2 == null || edgeTo1 == null) {
      throw new IllegalStateException();
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
      Set<T> visited = new HashSet<>(); 
      return isAPath(from, to, visited);
    }
    return false;
  }

  private boolean isAPath(T from, T to, Set<T> visited) {
    visited.add(from); 
    if (from.equals(to)) {
      return true;
    }
    
    for (Edge<T> edge : getEdgesFrom(from)) {
      if (!visited.contains(edge.getDestination())) {  
        if (isAPath(edge.getDestination(), to, visited)) {
          return true;
        }
      }  
    }
       return false;
  }

  @Override
  public List<Edge<T>> getPath(T from, T to) {
    Map<T, T> connection = new HashMap<>();
    recursiveConnect(from, null, connection);
    LinkedList<Edge<T>> path = new LinkedList<>();
    T current = to;
    while (current != null && !current.equals(from)) {
      T next = connection.get(current);
      if (next != null) {  
        Edge<T> edge = getEdgeBetween(next, current);
        path.addFirst(edge);
      }
      current = next;
    }
    return current == null ? null : path; 
  }

  private void recursiveConnect(T to, T from, Map<T, T> connection) {
    connection.put(to, from);
    for (Edge<T> edge : getEdgesFrom(to)) {
      if (!connection.containsKey(edge.getDestination())) {
        recursiveConnect(edge.getDestination(), to, connection);
      }
      
    }
  }
}
