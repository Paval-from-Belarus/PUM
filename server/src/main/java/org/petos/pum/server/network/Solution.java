package org.petos.pum.server.network;


import java.util.*;

/**
 * @author Paval Shlyk
 * @since 17/08/2023
 */
public class Solution {
public static void main(String[] args) {
      int[][] numbers = {
	  {7, 8}, {12, 16}, {12, 5}, {1, 8}, {4, 19}, {3, 15}, {4, 10}, {9, 16}
      };
      var instance = new Solution();
      instance.minScoreTriangulation(new int[]{
	  1, 2, 3
      });
      instance.test(new int[][]{
	  {1, 0, 0, 0},
	  {0, 0, 0, 0},
	  {0, 0, 0, 2}
      });
      byte[] number = multiply(
	  new char[]{9, 9, 9}, (char) 4
      );

}

private static class Enveloper {
      private int index;
      private int currCnt; //exactly stores in matrix

      private Enveloper(int index, int currCnt) {
	    this.index = index;
	    this.currCnt = currCnt;
      }

}

private static final Comparator<int[]> comparator = (left, right) -> {
      if (left[0] == right[0]) {
	    return left[1] - right[1];
      }
      return left[0] - right[0];
};

public int maxEnvelopes(int[][] envelopes) {
      Arrays.sort(envelopes, comparator);
      int[] lengths = new int[envelopes.length];
      for (int i = 0; i < envelopes.length; i++) {
	    for (int j = i + 1; j < envelopes.length; j++) {
		  if (envelopes[i][0] < envelopes[j][0] && envelopes[i][1] < envelopes[j][1] && lengths[j] < lengths[i] + 1) {
			lengths[j] = lengths[i] + 1;
		  }
	    }
      }
      return lengths[lengths.length - 1] + 1;
}

public int uniquePaths(int height, int width) {
      assert height > 0 && width > 0;
      if (height == 1 || width == 1) {
	    return 1;
      }
      int[] lastPath = new int[height];
      int[] currentPath = new int[height];
      Arrays.fill(lastPath, 1);
      for (int x = 0; x < width - 1; x++) { //first column was initially set
	    for (int y = 0; y < height; y++) {
		  currentPath[y] = lastPath[y];
		  if (y != 0) {
			currentPath[y] += currentPath[y - 1];
		  }
	    }
	    int[] temp = lastPath;
	    lastPath = currentPath;
	    currentPath = temp;
      }
      return lastPath[height - 1];
}


public int notUniquePathsWithObstacles(int[][] obstacleGrid) {
      int height = obstacleGrid.length;
      int width = obstacleGrid[0].length;
      if (obstacleGrid[height - 1][width - 1] == 1) {
	    return 0;
      }
//      setBounds(width, height);
//      setSource(obstacleGrid);
      return getPathsCnt(width - 1, height - 1);
}

public static final int PATH_EMPTY = Integer.MIN_VALUE;
public static final int PATH_BUSY = Integer.MIN_VALUE + 1;
private static final int POINT_START = 1;
private static final int POINT_END = 2;
private static final int POINT_EMPTY = 0;
public static final int POINT_OBSTACLE = -1;

private record Point(int x, int y) {
}

public int test(int[][] grid) {
      Set<Point> currentPoints = new HashSet<>();
      List<Point> startAndEnd = findStartAndEnd(grid);
      assert startAndEnd.size() == 2 && grid.length > 0;
      var start = startAndEnd.get(0);
      var end = startAndEnd.get(1);
      int height = grid.length;
      int width = grid[0].length;
      setBounds(width, height, grid, start);
      markAndTouch(startX, startY, currentPoints);
      this.pathMatrix[startY][startX] = 1;
      while (!currentPoints.isEmpty()) {
	    Set<Point> nextPoints = new HashSet<>();
	    List<Point> touchedPoints = new ArrayList<>();
	    for (Point point : currentPoints) {
		  int x = point.x();
		  int y = point.y();
		  List<Point> nearPoint = markAndTouch(x, y, nextPoints);
		  touchedPoints.addAll(nearPoint);
	    }
	    for (Point touched : touchedPoints) {
		  this.pathMatrix[touched.y()][touched.x()] += 1;
	    }
	    currentPoints = nextPoints;
      }
      return this.pathMatrix[end.y()][end.x()];

}

private List<Point> markAndTouch(int x, int y, Set<Point> nextPoints) {
      List<Point> touched = new ArrayList<>();
      final int[] xOffset = {0, 1, 0, -1};
      final int[] yOffset = {1, 0, -1, 0};
      final int DIMENSIONS_CNT = 4;
      int sum = 0;
      for (int i = 0; i < DIMENSIONS_CNT; i++) {
	    int nextX = x + xOffset[i];
	    int nextY = y + yOffset[i];
	    if (!isValidPos(nextX, nextY)) {
		  continue;
	    }
	    if (this.obstaclesMatrix[nextY][nextX] != POINT_OBSTACLE) {
		  if (this.pathMatrix[nextY][nextX] != 0) {
			sum += this.pathMatrix[nextY][nextX];
			touched.add(new Point(nextX, nextY));
			//increase value in path matrix that already was changed
			//each element in path matrix initially change in next generation wave
		  } else {
			nextPoints.add(new Point(nextX, nextY));
		  }
	    }
      }
      this.pathMatrix[y][x] = sum;
      return touched;
}

public int uniquePathsIII(int[][] grid) {
      int height = grid.length;
      int width = grid[0].length;
      List<Point> points = findStartAndEnd(grid);
      assert points.size() == 2;
      setBounds(width, height, grid, points.get(0));//start
      Point end = points.get(1);
      return getPathsCnt(end.x(), end.y());
}

private List<Point> findStartAndEnd(int[][] grid) {
      assert grid.length > 0;
      Point end = null;
      Point start = null;
      for (int y = 0; y < grid.length; y++) {
	    for (int x = 0; x < grid[0].length; x++) {
		  if (end == null && grid[y][x] == POINT_END) {
			end = new Point(x, y);
		  }
		  if (start == null && grid[y][x] == POINT_START) {
			start = new Point(x, y);
		  }
	    }
      }
      assert end != null && start != null;
      return List.of(start, end);
}


//this method doesn't perform any check for bounds limitation (for input)
private int getPathsCnt(int x, int y) {
      if (pathMatrix[y][x] != PATH_EMPTY) {
	    return pathMatrix[y][x];
      }
      final int[] xOffset = new int[]{-1, 0, 1, 0};
      final int[] yOffset = new int[]{0, -1, 0, 1};
      final int DIMENSIONS_CNT = 4;
      int sum = 0;
      pathMatrix[y][x] = PATH_BUSY;
      for (int i = 0; i < DIMENSIONS_CNT; i++) {
	    int nextX = x + xOffset[i];
	    int nextY = y + yOffset[i];
	    if (isValidPos(nextX, nextY) && (isFreePos(nextX, nextY) || isStartPos(nextX, nextY))) {
		  sum += getPathsCnt(nextX, nextY);
	    }
      }
      pathMatrix[y][x] = sum;
      return sum;
}


private void setBounds(int width, int height, int[][] obstaclesMatrix, Point start) {
      this.obstaclesMatrix = obstaclesMatrix;
      this.pathMatrix = new int[height][width];
//      for (int[] column : this.pathMatrix) {
//	    Arrays.fill(column, PATH_EMPTY);
//      }
      this.width = width;
      this.height = height;
      this.startX = start.x();
      this.startY = start.y();
      this.pathMatrix[this.startY][this.startX] = 1;
}

private boolean isValidPos(int nextX, int nextY) {
      return nextX >= 0 && nextX < width && nextY >= 0 && nextY < height;
}

private boolean isFreePos(int nextX, int nextY) {
      return obstaclesMatrix[nextY][nextX] == POINT_EMPTY && pathMatrix[nextY][nextX] == PATH_EMPTY;
}

private boolean isStartPos(int nextX, int nextY) {
      return startX == nextX && startY == nextY;
}

public String multiply(String num1, String num2) {
      char[] huge = num1.toCharArray();
      char[] small = num2.toCharArray();
      if (huge.length < small.length) {
	    char[] temp = huge;
	    huge = small;
	    small = temp;
      }


      return "";
}
public int minScoreTriangulation(int[] values) {
      final int EDGE_CNT = values.length;
      int[][] matrix = new int[EDGE_CNT][EDGE_CNT];
      for (int l = 2; l < EDGE_CNT; l++) {
	    for (int i = 0; i < EDGE_CNT - l; i++) {
		  int j = i + l;
		  matrix[i][j] = Integer.MAX_VALUE;
		  for (int k = i; k < j; k++) {
			int weight = values[k] * values[j] * values[i];
			int q = matrix[i][k] + matrix[k + 1][j] + weight;
			if (q < matrix[i][j]) {
			      matrix[i][j] = q;

			}
		  }
	    }
      }
      return matrix[0][EDGE_CNT - 1];
}
private static byte[] multiply(char[] number, char digit) {
      assert number.length > 0;
      int outputSize = number.length;
      if (number[number.length - 1] * digit > 10) {
	    outputSize += 1;
      }
      byte[] outputNumber = new byte[outputSize];
      int index = 0;
      int carryDigits = 0;
      for (char numberDigit : number) {
	    int value = (numberDigit * digit + carryDigits);
	    carryDigits = value / 10;
	    outputNumber[index] = (byte) (value % 10);
	    index += 1;
      }
      if (carryDigits > 0) {
	    outputNumber[index] = (byte) carryDigits;
      }
      return outputNumber;
}

private int[][] pathMatrix;
private int[][] obstaclesMatrix;
private int width;
private int height;
private int startX;
private int startY;
}
