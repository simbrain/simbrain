/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.projection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * An n-dimensional generalization of a simple QuadTree structure. This is a
 * binary tree that splits elements based on successive dimensions, repeating as
 * necessary. There are two types of nodes in the structure, branches and
 * leaves. The leaf nodes are a n-dimensional 'space' that contains a number of
 * elements. When an element is added that increases the number of points in
 * that leaf beyond the threshold specified by MAX, the leaf is split into two
 * new leaves attached to a new branch that replaces the old leaf. In splitting
 * a leaf, a mid-point is determined which places approximately half of the
 * points from the old leaf elements in each new leaf.
 *
 * <p>
 * In searching for an element, the mid-point of each branch is used to
 * determine the path through the tree. When the point to be searched is closer
 * than the given tolerance to the midpoint, the other branch is also followed
 * for correctness.
 *
 * <p>
 * A couple of other standard collections are used to provide efficient
 * index-based access and for reverse lookups of leafs.
 *
 * @author James Matthew Watson - July 2, 2007
 */
public class NTree implements Iterable<DataPoint> {

    /** The number of elements to allow in a leaf before splitting */
    static final int MAX = 50; /*
                                * determined ad hoc testing and hand-waving
                                * optimization theories
                                */

    /** The static logger for this class */
    private static final Logger LOGGER = Logger.getLogger(NTree.class);

    /** An instance specific logger */
    private Logger logger = LOGGER;

    /** An enumeration for quick switching on the node type */
    private enum Type {
        branch, leaf
    };

    /** The root node, initialized to a leaf */
    private Node root = new Leaf();

    /** The number of dimensions this structure supports */
    public final int dimensions;

    /** Indexed list of all elements */
    private List<DataPoint> list = new ArrayList<DataPoint>();

    /** Map of all elements mapped to their leafs */
    private Map<DataPoint, Leaf> all = new LinkedHashMap<DataPoint, Leaf>();

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
     * Returns the number of points in the tree.
     *
     * @return the number of points in the tree
     */
    public int size() {
        return all.size();
    }

    /**
     * Adds a point to the set.
     *
     * @param point the point to add
     * @return
     */
    public DataPoint add(DataPoint point) {
        if (logger.isDebugEnabled()) {
            logger.debug("adding point " + point);
        }

        /* Keeps track of the most recent parent branch, if any */
        Branch parent = null;

        /* The current node in the search, initialized to the root */
        Node current = root;
        /*
         * Keeps track of whether the current node is on the left or right of
         * it's parent.
         */
        boolean onLeft = true;

        /*
         * Iterates as long as the current node is a branch setting current to
         * left or right based on the midpoint of the branches split dimension.
         */
        while (current.type == Type.branch) {
            /* Cast current to Branch and set the parent */
            Branch branch = (Branch) current;
            parent = branch;

            if (point.get(branch.splitDimension) < branch.midPoint) {
                if (logger.isDebugEnabled())
                    logger.debug("at branch : " + branch + " - going left");
                /* To the left */
                current = branch.left;
                onLeft = true;
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("at branch : " + branch + " - going right");
                /* To the right */
                current = branch.right;
                onLeft = false;
            }
        }

        /* Cast the current node to a leaf */
        Leaf leaf = (Leaf) current;

        if (logger.isDebugEnabled())
            logger.debug("adding point to leaf : " + leaf);

        /* Add the point to the leaf and the list and map */
        leaf.points.add(point);
        list.add(point);
        all.put(point, leaf);

        /* check the number of points in the leaf */
        int size = leaf.points.size();

        if (logger.isDebugEnabled())
            logger.debug("leaf size : " + size);

        if (size > MAX) {
            /*
             * the leaf has more elements than the threshold. The leaf will be
             * split
             */
            int splitOn = parent == null ? 0 : (parent.splitDimension + 1)
                    % dimensions;
            if (logger.isDebugEnabled())
                logger.debug("splitting leaf on dimension: " + splitOn);

            /* Get the middle point index */
            int middle = size / 2;
            if (logger.isTraceEnabled())
                logger.trace("middle: " + middle);

            /* Sort the points based on the split dimension */
            Collections.sort(leaf.points, new PointComparator(splitOn));

            /*
             * Take the right most point on the left the left most point on the
             * right
             */
            DataPoint leftPoint = leaf.points.get(middle);
            if (logger.isTraceEnabled())
                logger.trace("leftPoint: " + leftPoint);

            DataPoint rightPoint = leaf.points.get(middle + 1);
            if (logger.isTraceEnabled())
                logger.trace("rightPoint: " + rightPoint);

            /*
             * Get the average between the points on the split dimension. this
             * is the midpoint
             */
            double midPoint = (leftPoint.get(splitOn) + rightPoint.get(splitOn)) / 2;
            if (logger.isTraceEnabled())
                logger.trace("midPoint: " + midPoint);

            /* instantiate the new branch with the midpoint and split-dimension */
            Branch newBranch = new Branch(midPoint, splitOn);

            /* create the new leaves */
            Leaf left = new Leaf();
            Leaf right = new Leaf();

            /* loop through all the points and add to the appropriate leaf */
            for (int i = 0; i < size; i++) {
                DataPoint p = leaf.points.get(i);

                if (p.get(splitOn) < midPoint) {
                    if (logger.isTraceEnabled())
                        logger.trace("adding to left: " + p);
                    left.points.add(p);
                    all.put(p, left);
                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("adding to right: " + p);
                    right.points.add(p);
                    all.put(p, right);
                }
            }

            /* set the new branches */
            newBranch.left = left;
            newBranch.right = right;

            /*
             * Set the branch on it's parent, unless there is none: then it's
             * the new root
             */
            if (parent == null) {
                if (logger.isTraceEnabled())
                    logger.debug("setting new branch as root");
                root = newBranch;
            } else if (onLeft) {
                if (logger.isTraceEnabled())
                    logger.debug("setting new branch as left");
                parent.left = newBranch;
            } else {
                if (logger.isTraceEnabled())
                    logger.debug("setting new branch as right");
                parent.right = newBranch;
            }
        }

        return null;
    }

    /**
     * Comparator used to compare two points on a single dimension
     */
    private static final class PointComparator implements Comparator<DataPoint> {

        /** The dimension to compare on */
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
        public int compare(DataPoint o1, DataPoint o2) {
            double difference = o1.get(dimension) - o2.get(dimension);
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
     * @param index of element to return.
     * @return Array of element at index location
     */
    public DataPoint get(final int index) {
        return list.get(index);
    }

    /**
     * Checks whether the given point already exists in the tree with the
     * specified tolerance.
     *
     * @param point the point to search for
     * @param tolerance the tolerance for determining uniqueness
     * @return whether the point is unique
     */
    public DataPoint isUnique(final DataPoint point, final double tolerance) {
        return isUnique(root, point, tolerance);
    }

    /**
     * Checks whether the given point already exists in the tree with the
     * specified tolerance.
     *
     * @param from the node to start from
     * @param point the point to search for
     * @param tolerance the tolerance for determining uniqueness
     * @return whether the point is unique
     */
    private DataPoint isUnique(Node from, DataPoint point, double tolerance) {

        if (logger.isDebugEnabled())
            logger.debug("is unique? tolerance " + tolerance + " - " + point);

        /* loop over the from node while it's a branch */
        while (from.type == Type.branch) {

            /* cast to a branch */
            Branch branch = (Branch) from;
            /* get the split dimension */
            double d = point.get(branch.splitDimension);

            /*
             * If the point is within tolerance of the split, recurse both paths
             * otherwise continue branching
             */
            if (Math.abs(d - branch.midPoint) < tolerance) {
                if (logger.isDebugEnabled())
                    logger.debug("at branch : " + branch
                            + " - recursing both paths");
                DataPoint leftCheck = isUnique(branch.left, point, tolerance);
                DataPoint rightCheck = isUnique(branch.right, point, tolerance);
                if ((leftCheck == null) && (rightCheck == null)) {
                    return null;
                } else {
                    if (leftCheck != null) {
                        return leftCheck;
                    } else {
                        return rightCheck;
                    }
                }
            } else if (point.getVector()[branch.splitDimension] < branch.midPoint) {
                if (logger.isDebugEnabled())
                    logger.debug("at branch : " + branch + " - going left");
                from = branch.left;
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("at branch : " + branch + " - going right");
                from = branch.right;
            }
        }

        /* cast to leaf */
        Leaf leaf = (Leaf) from;

        /*
         * loop over the points. if each of the elements in the point is within
         * a tolerance of the given point, check the distance. otherwise, the
         * point cannot be within a tolerance distance of the given point.
         */
        for (DataPoint p : leaf.points) {
            for (int i = 0; i < p.getDimension(); i++) {
                if (Math.abs(p.get(i) - point.get(i)) >= tolerance) {
                    break;
                }
            }

            /* If the distance is less than tolerance, this point is not unique */
            if (getDistance(p, point) < tolerance) {
                return p;
            }
        }

        /*
         * All possibilities in the current path have been exhausted and no
         * duplicates were found.
         */
        return null;
    }

    /**
     * Determines the Euclidean distance between two points.
     *
     * @param a First point of distance
     * @param b Second point of distance
     *
     * @return the Euclidean distance between points 1 and 2
     */
    public static double getDistance(final DataPoint a, final DataPoint b) {
        if (a.getDimension() != b.getDimension()) {
            throw new IllegalArgumentException(
                    "points of different dimensions cannot be compared: "
                            + a.getDimension() + ", " + b.getDimension());
        }

        double sum = 0;

        for (int i = 0; i < a.getDimension(); i++) {
            double difference = a.getVector()[i] - b.getVector()[i];
            sum += (difference * difference);
        }

        return Math.sqrt(sum);
    }

    /**
     * Gets the closest points to the passed in point. The amount of points to
     * determine is specified by the number argument
     *
     * @param number the number of points to collect
     * @param point the point to find points close to
     * @return the closest points
     */
    public List<DataPoint> getClosestPoints(int number, DataPoint point) {
        List<DataPoint> points = new ArrayList<DataPoint>();
        for (DistancePoint dp : getClosestPoints(root, number, point)) {
            points.add(dp.point);
        }

        return points;
    }

    /**
     * Gets the closest points to the passed in point. The amount of points to
     * determine is specified by the number argument
     *
     * @param from then node to start from
     * @param number the number of points to collect
     * @param point the point to find points close to
     * @return the closest points
     */
    private List<DistancePoint> getClosestPoints(Node from, int number,
            DataPoint point) {
        List<DistancePoint> points;

        /*
         * if from is a branch, recurse otherwise get the closest points in the
         * leaf
         */
        if (from.type == Type.branch) {
            /* cast to Branch */
            Branch branch = (Branch) from;
            /* the point's value on the splitDimension */
            double d = point.get(branch.splitDimension);
            /* determine whether the normal path is left or right */
            boolean left = d < branch.midPoint;

            /* recurse on branch determined above */
            points = getClosestPoints(left ? branch.left : branch.right,
                    number, point);

            /*
             * determine whether to recurse on the other path. if the farthest
             * out point from the main branch is less than the distance to the
             * split, get the n points from the other branch
             */
            if (points.size() < number
                    || points.get(number - 1).distance > Math.abs(d
                            - branch.midPoint)) {
                points.addAll(getClosestPoints(left ? branch.right
                        : branch.left, number, point));

                /* combine the points and sort */
                Collections.sort(points, new Comparator<DistancePoint>() {
                    public int compare(DistancePoint o1, DistancePoint o2) {
                        if (o1.distance < o2.distance)
                            return -1;
                        else if (o1.distance > o2.distance)
                            return 1;
                        else
                            return 0;
                    }
                });
            }
        } else {
            points = new ArrayList<DistancePoint>();
            /* cast to Leaf */
            Leaf leaf = (Leaf) from;

            /*
             * loop over the points in the leaf adding any that are less than
             * the current or adding if there are less than n
             */
            for (DataPoint d : leaf.points) {
                double distance = getDistance(d, point);

                for (int i = 0; i < number; i++) {
                    if (i >= points.size()) {
                        points.add(new DistancePoint(distance, d));
                    } else if (distance < points.get(i).distance) {
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
        DataPoint point;

        /**
         * @param distance the distance to the given point
         * @param point a point
         */
        DistancePoint(final double distance, final DataPoint point) {
            this.distance = distance;
            this.point = point;
        }
    }

    /**
     * Returns the closest point in the tree to the given point.
     *
     * @param point
     * @return the point closest to the given point
     */
    public DataPoint getClosestPoint(final DataPoint point) {
        return getClosestPoints(1, point).get(0);
    }

    /**
     * Returns the index for the given point.
     *
     * @param point the point to lookup
     * @return the index of that point
     */
    public int getIndex(DataPoint point) {
        return list.indexOf(point);
    }

    /**
     * returns the tree as an arraylist. This returned list is ordered by index
     *
     * @return the tree as an arraylist
     */
    public ArrayList<DataPoint> asArrayList() {
        return new ArrayList<DataPoint>(list);
    }

    /**
     * adds all the elements from the given tree to this tree
     *
     * @param other the other tree
     */
    public void addAll(NTree other) {
        for (DataPoint d : other) {
            add(d);
        }
    }

    /**
     * Returns an iterator over this tree
     */
    public Iterator<DataPoint> iterator() {
        return list.iterator();
    }

    /**
     * replaces the point at the given index with the one provided.
     *
     * @param index the index to set the point at
     * @param point the point to set
     */
    public void set(int index, DataPoint point) {
        DataPoint old = list.get(index);
        Leaf leaf = all.get(old); // leaf can be null sometimes..
        if (leaf == null) {
            System.out.println(index);
        }
        int leafIndex = leaf.points.indexOf(old);
        leaf.points.set(leafIndex, point);
        all.put(point, leaf);
        all.remove(old);
        list.set(index, point);
    }

    /*----------------------------------------------*/

    /**
     * Base class for nodes.
     */
    private abstract static class Node {
        Type type;
    }

    /**
     * Class for branches.
     */
    private static class Branch extends Node {
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
     * Class for leaves.
     */
    private static class Leaf extends Node {
        {
            type = Type.leaf;
        }

        List<DataPoint> points = new ArrayList<DataPoint>();

        public String toString() {
            return "size: " + points.size();
        }
    }
}