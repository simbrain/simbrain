package org.simbrain.gauge.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * An n-dimensional generalization of a simple QuadTree structure
 * This is a binary tree that splits elements based on successive 
 * dimensions, repeating as necessary.  There are two types of nodes in 
 * the structure, branches and leaves.  The leaf nodes are a n-dimensional
 * 'space' that contains a number of elements.  When an element is added
 * that increases the number of points in that leaf beyond the threshold
 * specified by MAX, the leaf is split into two new leaves attached to a
 * new branch that replaces the old leaf.  In splitting a leaf, a mid-point
 * is determined which places approximately half of the points from the old 
 * leaf elements in each new leaf.
 * 
 * <p>In searching for an element, the mid-point of each branch is used to
 * determine the path through the tree.  When the point to be searched is
 * closer than the given tolerance to the midpoint, the other branch is also 
 * followed for correctness.
 * 
 * <p>A couple of other standard collections are used to provide efficient
 * index-based access and for reverse lookups of leafs.
 * 
 * @author James Matthew Watson - July 2, 2007
 */
public class NTree implements Iterable<double[]> {
    /** the number of elements to allow in a leaf before splitting */
    static final int MAX = 50; /* determined ad hoc testing and hand-waving optimization theories */
    
    /** the static logger for this class */
    private static final Logger LOGGER = Logger.getLogger(NTree.class);
    /** an instance specific logger */
    private Logger logger = LOGGER;
    
    /** an enumeration for quick switching on the node type */
    private enum Type {branch, leaf};
    /** the root node, initialized to a leaf */
    private Node root = new Leaf();
    /** the number of dimensions this structure supports */
    public final int dimensions;
    /** indexed list of all elements */
    private List<double[]> list = new ArrayList<double[]>();
    /** map of all elements mapped to their leafs */
    private Map<double[], Leaf> all = new LinkedHashMap<double[], Leaf>();
    
    /**
     * Constructs an NTree with the given number of dimensions
     * 
     * @param dimensions the number of dimensions
     */
    NTree(int dimensions) {
        LOGGER.debug("Creating an NTree with " + dimensions + " dimensions.");
        this.dimensions = dimensions;
        logger = Logger.getLogger(logger.getName() + '.' + dimensions);
    }
    
    /**
     * returns the number of points in the tree
     * 
     * @return the number of points in the tree
     */
    public int size() {
        return all.size();
    }
    
    /**
     * Adds a point to the set
     * 
     * @param point the point to add
     */
    public boolean add(double[] point) {
        if (logger.isDebugEnabled()) logger.debug("adding point " + toString(point));
        /* keeps track of the most recent parent branch, if any */
        Branch parent = null;
        /* the current node in the search, initialized to the root */
        Node current = root;
        /* keeps track of whether the current node is on the left or right of it's parent */
        boolean onLeft = true;

        /* 
         * Iterates as long as the current node is a branch setting current 
         * to left or right based on the midpoint of the branches split dimension.
         */
        while (current.type == Type.branch) {
            /* cast current to Branch and set the parent */
            Branch branch = (Branch) current;
            parent = branch;
            
            if (point[branch.splitDimension] < branch.midPoint) {
                if (logger.isDebugEnabled()) logger.debug("at branch : " + branch + " - going left");
                /* to the left */
                current = branch.left;
                onLeft = true;
            } else {
                if (logger.isDebugEnabled()) logger.debug("at branch : " + branch + " - going right");
                /* to the right */
                current = branch.right;
                onLeft = false;
            }
        }
        
        /* cast the current node to a leaf */
        Leaf leaf = (Leaf) current;
        
        if (logger.isDebugEnabled()) logger.debug("adding point to leaf : " + leaf);
        
        /* add the point to the leaf and the list and map */
        leaf.points.add(point);
        list.add(point);
        all.put(point, leaf);
        
        /* check the number of points in the leaf */
        int size = leaf.points.size();
        
        if (logger.isDebugEnabled()) logger.debug("leaf size : " + size);
        
        if (size > MAX) {
            /* the leaf has more elements than the threshold.  The leaf will be split */
            int splitOn = parent == null ? 0 : (parent.splitDimension + 1) % dimensions;
            if (logger.isDebugEnabled()) logger.debug("splitting leaf on dimension: " + splitOn);
            
            /* get the middle point index */
            int middle = size / 2;
            if (logger.isTraceEnabled()) logger.trace("middle: " + middle);
            
            /* sort the points based on the split dimension */
            Collections.sort(leaf.points, new PointComparator(splitOn));
            
            /* take the right most point on the left the left most point on the right */
            double[] leftPoint = leaf.points.get(middle);
            if (logger.isTraceEnabled()) logger.trace("leftPoint: " + toString(leftPoint));
            
            double[] rightPoint = leaf.points.get(middle + 1);
            if (logger.isTraceEnabled()) logger.trace("rightPoint: " + toString(rightPoint));
            
            /* get the average between the points on the split dimension. this is the midpoint */
            double midPoint = (leftPoint[splitOn] + rightPoint[splitOn]) / 2;
            if (logger.isTraceEnabled()) logger.trace("midPoint: " + midPoint);
            
            /* instantiate the new branch with the midpoint and split-dimension */
            Branch newBranch = new Branch(midPoint, splitOn);
            
            /* create the new leaves */
            Leaf left = new Leaf();
            Leaf right = new Leaf();
            
            /* loop through all the points and add to the appropriate leaf */
            for (int i = 0; i < size; i++) {
                double[] p = leaf.points.get(i);
                
                if (p[splitOn] < midPoint) {
                    if (logger.isTraceEnabled()) logger.trace("adding to left: " + toString(p));
                    left.points.add(p);
                    all.put(p, left);
                } else {
                    if (logger.isTraceEnabled()) logger.trace("adding to right: " + toString(p));
                    right.points.add(p);
                    all.put(p, right);
                }
            }
            
            /* set the new braches */
            newBranch.left = left;
            newBranch.right = right;
            
            /* set the brach on it's parent, unless there is none: then it's the new root */
            if (parent == null) {
                if (logger.isTraceEnabled()) logger.debug("setting new branch as root");
                root = newBranch;
            } else if (onLeft) {
                if (logger.isTraceEnabled()) logger.debug("setting new branch as left");
                parent.left = newBranch;
            } else {
                if (logger.isTraceEnabled()) logger.debug("setting new branch as right");
                parent.right = newBranch;
            }
        }
        
        /* this method should always return true unless an exception is thrown */
        return true;
    }
    
    /**
     * pretty prints the point
     */
    private String toString(double[] point) {
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < point.length; i++) {
            builder.append(point[i]);
            if (i < point.length - 1) builder.append(", ");
        }
        
        return builder.toString();
    }
    
    /**
     * Comparator used to compare two points on a single dimension
     */
    private static final class PointComparator implements Comparator<double[]> {
        /** the dimension to compare on */
        final int dimension;
        
        /**
         * Constructs a new comparator on the given dimension
         * 
         * @param dimension the dimension to compare on 
         */
        PointComparator(int dimension) {
            this.dimension = dimension;
        }

        /**
         * compares two points on one dimension
         */
        public int compare(double[] o1, double[] o2) {
            double difference = o1[dimension] - o2[dimension];
            if (difference < 0) {
                return -1;
            } else if (difference > 0) {
                return 1;
            } else {
                return 0;
            }
        }  
    }
    
    /**
     * 
     * @param index
     * @return
     */
    public double[] get(int index) {
        return list.get(index);
    }
    
    /**
     * checks whether the given point already exists in the tree
     * with the specified tolerance
     * 
     * @param point the point to search for
     * @param tolerance the tolerance for determining uniqueness
     * @return whether the point is unique
     */
    public boolean isUnique(double[] point, double tolerance) {
        return isUnique(root, point, tolerance);
    }
    
    /**
     * Checks whether the given point already exists in the tree
     * with the specified tolerance.
     *
     * @param from the node to start from
     * @param point the point to search for
     * @param tolerance the tolerance for determining uniqueness
     * @return whether the point is unique
     */
    private boolean isUnique(Node from, double[] point, double tolerance) {

        if (logger.isDebugEnabled()) logger.debug("is unique? tolerance " + tolerance + " - " + toString(point));

        /* loop over the from node while it's a branch */
        while (from.type == Type.branch) {

            /* cast to a branch */
            Branch branch = (Branch) from;
            /* get the split dimension */
            double d = point[branch.splitDimension];

            /*
             * if the point is within tolerance of the split, recurse both paths
             * otherwise continue braching
             */
            if (Math.abs(d - branch.midPoint) < tolerance) {
                if (logger.isDebugEnabled()) logger.debug("at branch : " + branch + " - recursing both paths");
                return isUnique(branch.left, point, tolerance) 
                  && isUnique(branch.right, point, tolerance);
            } else if (point[branch.splitDimension] < branch.midPoint) {
                if (logger.isDebugEnabled()) logger.debug("at branch : " + branch + " - going left");
                from = branch.left;
            } else {
                if (logger.isDebugEnabled()) logger.debug("at branch : " + branch + " - going right");
                from = branch.right;
            }
        }
        
        /* cast to leaf */
        Leaf leaf = (Leaf) from;
        
        /* 
         * loop over the points.  if each of the elements in the point is
         * within a tolerance of the given point, check the distance.
         * otherwise, the point cannot be within a tolerance distance
         * of the given point.
         */
        for (double[] p : leaf.points) {
            for (int i = 0; i < p.length; i++) {
                if (Math.abs(p[i] - point[i]) >= tolerance) {
                    break;
                }
            }
            
            /* if the distance is less than tolerance, this point is not unique */
            if (getDistance(p, point) < tolerance) {
                return false;
            }
        }

        /* 
         * All possiblities in the current path have been exhausted
         * and no dupes were found.
         */
        return true;
    }

    /**
     * determines the euclidean distance between two points.
     *
     * @param a First point of distance
     * @param b Second point of distance
     * 
     * @return the Euclidean distance between points 1 and 2
     */
    public static double getDistance(final double[] a, final double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("points of different dimensions cannot be compared: " 
                    + a.length + ", " + b.length);
        }

        double sum = 0;

        for (int i = 0; i < a.length; i++) {
            double difference = a[i] - b[i];
            sum += (difference * difference);
        }

        return Math.sqrt(sum);
    }
    
    /**
     * gets the closest points to the passed in point.  The amount
     * of points to determine is specified by the number argument
     * 
     * @param number the number of points to collect
     * @param point the point to find points close to
     * @return the closest points
     */
    public List<double[]> getClosestPoints(int number, double[] point) {
        List<double[]> points = new ArrayList<double[]>();
        for (DistancePoint dp : getClosestPoints(root, number, point)) {
            points.add(dp.point);
        }
        
        return points;
    }
    
    /**
     * gets the closest points to the passed in point.  The amount
     * of points to determine is specified by the number argument
     * 
     * @param from then node to start from
     * @param number the number of points to collect
     * @param point the point to find points close to
     * @return the closest points
     */
    private List<DistancePoint> getClosestPoints(Node from, int number, double[] point) {
        List<DistancePoint> points;
        
        /* 
         * if from is a branch, recurse otherwise get the closest points in the
         * leaf 
         */
        if (from.type == Type.branch) {
            /* cast to Branch */
            Branch branch = (Branch) from;
            /* the point's value on the splitDimension */
            double d = point[branch.splitDimension];
            /* determine whether the normal path is left or right */
            boolean left = d < branch.midPoint;
            
            /* recurse on brach determined above */
            points = getClosestPoints(left ? branch.left : branch.right, number, point);
            
            /* 
             * determine whether to recurse on the other path.  if the farthest out 
             * point from the main branch is less than the distance to the split, 
             * get the n points from the other branch
             */
            if (points.size() < number || points.get(number - 1).distance > Math.abs(d - branch.midPoint)) {
                points.addAll(getClosestPoints(left ? branch.right : branch.left, number, point));
                
                /* combine the points and sort */
                Collections.sort(points, new Comparator<DistancePoint>() {
                    public int compare(DistancePoint o1, DistancePoint o2) {
                        if (o1.distance < o2.distance) return -1;
                        else if (o1.distance > o2.distance) return 1;
                        else return 0;
                    }
                });
            }
        } else {
            points = new ArrayList<DistancePoint>();
            /* cast to Leaf */
            Leaf leaf = (Leaf) from;
            
            /*
             * loop over the points in the leaf
             * adding any that are less than the current
             * or adding if there are less than n
             */
            for (double[] d : leaf.points) {
                double distance = getDistance(d, point);
                
                for (int i = 0; i < number; i++) {
                    if (i >= points.size()) {
                        points.add(new DistancePoint(distance, d));
                    } else if (distance < points.get(i).distance){
                        points.add(i, new DistancePoint(distance, d));
                    }
                }
            }
        }
        
        /* trim the list to size, if necessary */
        return points.size() < number ? points : points.subList(0, number);
    }
    
    /**
     * A tuple of a point and the distance to that point
     */
    private static class DistancePoint {
        double distance;
        double[] point;
        
        /**
         * @param distance the distance to the given point
         * @param point a point
         */
        DistancePoint(final double distance, final double[] point) {
            this.distance = distance;
            this.point = point;
        }   
    }
    
    /**
     * returns the closest point in the tree to the given point
     * 
     * @param point
     * @return the point closest to the given point
     */
    public double[] getClosestPoint(final double[] point) {
        return getClosestPoints(1, point).get(0);
    }
    
    /**
     * returns the insdex for the given point
     * 
     * @param point the point to lookup
     * @return the index of that point
     */
    public int getIndex(double[] point)
    {
        return list.indexOf(point);
    }
    
    /**
     * returns the tree as an arraylist.  This
     * returned list is ordered by index
     * 
     * @return the tree as an arraylist
     */
    public ArrayList<double[]> asArrayList() {
        return new ArrayList<double[]>(list);
    }
    
    /**
     * adds all the elements from the given tree
     * to this tree
     * 
     * @param other the other tree
     */
    public void addAll(NTree other) {
        for (double[] d : other) {
            add(d);
        }
    }
    
    /**
     * returns an iterator over this tree
     */
    public Iterator<double[]> iterator() {
        return list.iterator();
    }
    
    /**
     * replaces the point at the given index with the one provided
     * 
     * @param index the index to set the point at
     * @param point the point to set
     */
    public void set(int index, double[] point) {
        double[] old = list.get(index);
        Leaf leaf = all.get(old);
        int leafIndex = leaf.points.indexOf(old);
        
        leaf.points.set(leafIndex, point);
        all.put(point, leaf);
        all.remove(old);
        list.set(index, point);
    }
    
    /*----------------------------------------------*/ 
   
    /**
     * base class for nodes
     */
    private abstract static class Node {
        Type type;
    }
    
    /**
     * class for braches
     */
    private static class Branch extends Node
    {
        Node left;
        Node right;
        final double midPoint;
        final int splitDimension;
        
        Branch(double midPoint, int splitDimension) {
            type = Type.branch;
            this.midPoint = midPoint;
            this.splitDimension = splitDimension;
        }
        
        public String toString() {
            return "split on: " + splitDimension + ", midPoint: " + midPoint;
        }
    }
    
    /**
     * class for leaves
     */
    private static class Leaf extends Node
    {
        {type = Type.leaf;}
        
        List<double[]> points = new ArrayList<double[]>();
        
        public String toString() {
            return "size: " + points.size();
        }
    }
}